from faster_whisper import WhisperModel

_model =None

def get_model():
    global _model
    if _model is None:
        _model = WhisperModel("small", device="cpu", compute_type="int8")
    return _model

def transcribe(audio_path: str):
    model = get_model()
    segments, info = model.transcribe(audio_path, vad_filter=True)
    out = []
    for s in segments:
        out.append({
            "start": float(s.start)
            ,"end": float(s.end),
            "text": s.text.strip()
        })
    language = info.language or "unknown"
    return language, out