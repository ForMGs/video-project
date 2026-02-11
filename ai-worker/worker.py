import json
import os
import time
import traceback
import tempfile

import redis
import pymysql
from dotenv import load_dotenv

from storage import download_from_minio
from media import extract_audio
from stt import transcribe

# ✅ OpenAI(Structured Outputs) 기반 챕터 생성 함수
from llm_provider import make_chapters_from_segments

# 1) Redis 큐에서 job_id 받기
# 2) DB에서 해당 row 잠금
# 3) PENDING/FAILED면 처리 가능, RUNNING/DONE이면 스킵
# 4) RUNNING으로 바꾸면서 attempt = attempt + 1
# 5) 처리 성공 -> DONE + 결과 저장
# 6) 처리 실패 -> FAILED + error_message 저장

load_dotenv()

REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379/0")
QUEUE_KEY = os.getenv("QUEUE_KEY", "ai:jobs")

DB_HOST = os.getenv("DB_HOST", "mysql")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_USER = os.getenv("DB_USER", "root")
DB_PASS = os.getenv("DB_PASS", "password")
DB_NAME = os.getenv("DB_NAME", "app")

rds = redis.Redis.from_url(REDIS_URL, decode_responses=True)


def db_conn():
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASS,
        database=DB_NAME,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def select_job_for_update(conn, job_id: int):
    with conn.cursor() as cur:
        cur.execute("SELECT * FROM video_ai_result WHERE id=%s FOR UPDATE", (job_id,))
        return cur.fetchone()


def mark_running(conn, job_id: int):
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE video_ai_result
            SET status='RUNNING',
                attempt=attempt+1,
                error_message=NULL
            WHERE id=%s
            """,
            (job_id,),
        )


def mark_done(
    conn,
    job_id: int,
    language: str,
    transcript_json: str,
    summary_short: str | None,
    summary_long: str | None,
    chapters_json: str,
):
    # ✅ 파라미터 개수/순서 정확히 맞춤 (error_message를 VALUES에 넣지 않음)
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE video_ai_result
            SET status='DONE',
                language=%s,
                transcript_json=%s,
                summary_short=%s,
                summary_long=%s,
                chapters_json=%s,
                error_message=NULL
            WHERE id=%s
            """,
            (language, transcript_json, summary_short, summary_long, chapters_json, job_id),
        )


def mark_failed(conn, job_id: int, error_message: str):
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE video_ai_result
            SET status='FAILED',
                error_message=%s
            WHERE id=%s
            """,
            (error_message, job_id),
        )


def process_ai_pipeline(video_id: int):
    """
    현재 버전: MinIO 다운로드 → ffmpeg 오디오 추출 → Whisper STT → OpenAI로 chapters 생성 → DB 저장용 결과 반환
    """
    bucket = os.getenv("VIDEO_BUCKET", "videos")
    # object_key = os.getenv("VIDEO_OBJECT_KEY")  # ✅ 오타 수정: VIDEO_OBJECY_KEY -> VIDEO_OBJECT_KEY
    object_key = get_video_storage(conn, video_id)

    if not object_key:
        raise RuntimeError(
            "VIDEO_OBJECT_KEY is not set. Need bucket/object_key lookup by video_id "
            "or provide VIDEO_OBJECT_KEY env."
        )

    with tempfile.TemporaryDirectory() as tmp:
        video_path = os.path.join(tmp, "video.mp4")
        audio_path = os.path.join(tmp, "audio.wav")

        # 1) MinIO에서 영상 다운로드
        download_from_minio(bucket, object_key, video_path)

        # 2) 오디오 추출
        extract_audio(video_path, audio_path)

        # 3) STT
        language, segments = transcribe(audio_path)

        # 4) OpenAI로 chapters 생성 (Structured Outputs)
        chapters = make_chapters_from_segments(segments)

        return {
            "language": language,
            "transcript_json": json.dumps(segments, ensure_ascii=False),
            "summary_short": None,   # 필요하면 나중에 추가
            "summary_long": None,    # 필요하면 나중에 추가
            "chapters_json": json.dumps(chapters, ensure_ascii=False),
        }


def worker_loop():
    print("[worker] started. waiting jobs ...")
    while True:
        try:
            item = rds.brpop(QUEUE_KEY, timeout=0)  # (key, value)
            if not item:
                continue
            _, job_id_str = item
            job_id = int(job_id_str)

            conn = db_conn()
            try:
                conn.begin()
                job = select_job_for_update(conn, job_id)
                if not job:
                    conn.commit()
                    continue

                status = job["status"]
                if status in ("RUNNING", "DONE"):
                    conn.commit()
                    continue

                # PENDING/FAILED만 처리
                mark_running(conn, job_id)
                conn.commit()

                # 실제 파이프라인 실행
                result = process_ai_pipeline(job["video_id"])

                conn.begin()
                mark_done(
                    conn,
                    job_id,
                    result["language"],
                    result["transcript_json"],
                    result["summary_short"],
                    result["summary_long"],
                    result["chapters_json"],
                )
                conn.commit()
                print(f"[worker] job {job_id} DONE")

            except Exception as e:
                err = "".join(traceback.format_exception(type(e), e, e.__traceback__))
                try:
                    conn.rollback()
                    conn.begin()
                    mark_failed(conn, job_id, err)
                    conn.commit()
                except Exception:
                    pass
                print(f"[worker] job {job_id} FAILED\n{err}")

            finally:
                conn.close()

        except Exception as outer:
            print("[worker] loop error:", outer)
            time.sleep(1)

def get_video_storage(conn, video_id: int):
    with conn.cursor() as cur:
        cur.execute(
            "SELECT storage_key FROM video WHERE id=%s",(video_id,)
        )
        row = cur.fetchone()
        if not row or not row.get("storage_key"):
            raise RuntimeError(f"Video storage info not found for video_id={video_id}")
        return row["storage_key"]

if __name__ == "__main__":
    worker_loop()
