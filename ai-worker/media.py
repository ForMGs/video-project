import subprocess

def extract_audio(video_path:str, audio_path: str):
    cmd =[
        "ffmpeg", "-y",
        "-i", video_path,
        "-vn",
        "-ac","1",
        "-ar","16000",
        audio_path
    ]
    subprocess.run(cmd,check=True, stdout=subprocess.DEVNULL, stderr= subprocess.DEVNULL)