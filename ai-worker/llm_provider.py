import os
from openai import OpenAI
from schemas import ChaptersOut

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

MODEL = os.getenv("OPENAI_MODEL", "gpt-4o-mini")  # structured outputs 지원 모델 권장

def make_chapters_from_segments(segments) -> list[dict]:
    """
    segments: [{"start": float, "end": float, "text": str}, ...]
    returns:  [{"start": int, "title": str, "description": str?}, ...]
    """
    # 프롬프트는 '모델이 할 일'만 간단히 지시하고, 형식은 Pydantic/Structured Outputs로 강제한다.
    instructions = (
        "You generate video chapters from transcript segments. "
        "Create 5-12 chapters depending on length. "
        "Chapters must be chronological, cover the whole video, and not overlap. "
        "Use the same language as the transcript. "
        "Do not invent content."
    )

    user_input = {
        "segments": segments
    }

    # responses.parse: Pydantic 모델로 구조화 출력 강제
    resp = client.responses.parse(
        model=MODEL,
        instructions=instructions,
        input=user_input,
        text_format=ChaptersOut,
    )

    parsed: ChaptersOut = resp.output_parsed
    # DB에는 chapters 배열만 저장할 거라 list[dict]로 변환
    return [c.model_dump(exclude_none=True) for c in parsed.chapters]
