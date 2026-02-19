import { useEffect, useState, useRef, useMemo } from "react";
import { useParams, Link } from "react-router-dom";
import { fetchvideo } from "../api/video";
import type { VideoDetail } from "../types/video";
import Main from "../components/section/Main";
import { http } from "../api/http";

type Chapter = { start: number; title: string; description?: string };

async function fetchVideoById(id: string): Promise<VideoDetail> {
  try {
    return await fetchvideo(id);
  } catch {
    throw new Error("영상 정보를 불러오지 못했습니다.");
  }
}

const fmtViews = (n: number) =>
  new Intl.NumberFormat("ko-KR", { notation: "compact" }).format(n);

function formatTime(sec: number) {
  const m = String(Math.floor(sec / 60)).padStart(2, "0");
  const s = String(Math.floor(sec % 60)).padStart(2, "0");
  return `${m}:${s}`;
}

export default function WatchPage() {
  const { videoID } = useParams<{ videoID: string }>();

  const [video, setVideo] = useState<VideoDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [descOpen, setDescOpen] = useState(false);

  const didSend = useRef(false);

  const [durationSec, setDurationSec] = useState<number>(0);
  const videoRef = useRef<HTMLVideoElement | null>(null);

  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [aiStatus, setAiStatus] = useState<string>("NONE");
  const [aiError, setAiError] = useState<string>("");

  useEffect(() => {
    (async () => {
      try {
        if (!videoID) return;
        const v = await fetchVideoById(videoID);
        setVideo(v);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    })();
  }, [videoID]);

  useEffect(() => {
    if (didSend.current || !videoID) return;
    didSend.current = true;

    (async () => {
      try {
        await http.post(`/videos/${videoID}/view`);
      } catch (e) {
        console.error(e);
      }
    })();
  }, [videoID]);

  useEffect(() => {
    if (!videoID) return;

    let cancelled = false;
    let timer: number | null = null;

    const poll = async () => {
      try {
        const r = await http.get(`/videos/${videoID}/ai`);
        if (cancelled) return;

        const status = r.data.status as string;
        setAiStatus(status);

        if (status === "NONE") {
          await http.post(`/videos/${videoID}/ai`);
          if (cancelled) return;
          timer = window.setTimeout(poll, 30000);
          return;
        }

        if (status === "PENDING" || status === "RUNNING") {
          timer = window.setTimeout(poll, 3000);
          return;
        }

        if (status === "DONE") {
          setChapters(r.data.chapters || []);
          setAiError("");
          return;
        }

        if (status === "FAILED") {
          setAiError(r.data.errorMessage || "AI 챕터 생성에 실패했습니다.");
          return;
        }
      } catch (e) {
        if (!cancelled) timer = window.setTimeout(poll, 5000);
        console.error("ai 연동 에러 :", e);
      }
    };

    poll();
    return () => {
      cancelled = true;
      if (timer) window.clearTimeout(timer);
    };
  }, [videoID]);

  const segments = useMemo(() => {
    if (!chapters.length || !durationSec) return [];
    const sorted = [...chapters].sort((a, b) => a.start - b.start);

    return sorted.map((c, i) => {
      const start = Math.max(0, Math.floor(c.start));
      const end =
        i < sorted.length - 1 ? Math.floor(sorted[i + 1].start) : Math.floor(durationSec);
      return { start, end: Math.max(end, start + 1), title: c.title };
    });
  }, [chapters, durationSec]);

  const seekTo = (sec: number) => {
    const v = videoRef.current;
    if (!v) return;
    v.currentTime = sec;
    v.play();
  };

  if (loading) return <div className="watch-loading">로딩 중...</div>;
  if (!video) return <div className="watch-error">영상 정보를 불러올 수 없습니다.</div>;

  return (
    <Main title={video.title} description="">

        <div className="watch">
        <div className="watch__left">
            <video ref={videoRef} className="watch__player" controls src={video.videoUrl}
              onLoadedMetadata={()=> {
                const d= videoRef.current?.duration;
                if(d && Number.isFinite(d)) setDurationSec(d);
              }}/>
            {aiStatus==="PENDING" || aiStatus ==="RUNNING" ? (
              <div className="watch_aiNotice">챕터 생성 중...</div>
            ):null}
            {aiStatus==="FAILED"?(
              <div className="watch_aiError">{aiError}</div>
            ):null} 
            {/* ✅ 챕터 구간 바 + 리스트 */}
            {segments.length > 0 && durationSec > 0 && (
              <div className="watch__chapters">
                <div className="watch__chapterbar" aria-label="챕터 구간">
                  {segments.map((s) => {
                    const widthPct = ((s.end - s.start) / durationSec) * 100;
                    return (
                      <button
                        key={s.start}
                        className="watch__chapterseg"
                        style={{ width: `${widthPct}%` }}
                        title={`${formatTime(s.start)} · ${s.title}`}
                        onClick={() => seekTo(s.start)}
                      />
                    );
                  })}
                </div>

                <div className="watch__chapterlist">
                  {segments.map((s) => (
                    <button
                      key={s.start}
                      className="watch__chapteritem"
                      onClick={() => seekTo(s.start)}
                    >
                      <span className="watch__chaptertime">{formatTime(s.start)}</span>
                      <span className="watch__chaptertitle">{s.title}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            <h1 className="watch__title">{video.title}</h1>

            <div className="watch__meta">
            <div className="watch__channel">
                <img
                src={video.profileUrl || "/fallback-profile.png"}
                alt={video.userName}
                className="watch__avatar"
                />
                <div>
                <div className="watch__channel-name">{video.userName}</div>
                <div className="watch__info">
                    조회수 {fmtViews(video.views)} · {video.createdAt} ·{" "}
                    {video.durationSeconds}
                </div>
                </div>
                <Link to={`/channel/${video.userId}`} className="watch__btn">
                채널 이동
                </Link>
            </div>

            <div className="watch__actions">
                <button>공유</button>
                <button>저장</button>
            </div>
            </div>

            <div className="watch__desc">
            <p className={`watch__desc-text ${descOpen ? "open" : ""}`}>
                {video.description || "설명이 없습니다."}
            </p>
            {video.tags && (
              <div className="watch__tags">
                {video.tags
                  .split(",")                 // , 기준으로 나누기
                  .map(tag => tag.trim())     // 공백 제거
                  .filter(Boolean)            // 빈 문자열 제거
                  .map((tag, idx) => (
                    <Link key={idx} to={`/search?tag=${encodeURIComponent(tag)}`}>
                      #{tag}
                    </Link>
                  ))}
              </div>
            )}
            <button
                onClick={() => setDescOpen(!descOpen)}
                className="watch__more"
            >
                {descOpen ? "간략히" : "더보기"}
            </button>
            </div>
        </div>
        </div>
    </Main>
  );
}

