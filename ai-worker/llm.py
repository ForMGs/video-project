def load_prompt(path="prompts/chapters_promt.txt"):
    with open(path,"r", encoding="utf-8") as f:
        return f.read()

def build_prompt(segments):
    template = load_prompt()
    return template.replace("{SEGMENTS_JSON}", json.dumps(segments, ensure_ascii=False))
