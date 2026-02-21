# -*- coding: utf-8 -*-
# Durbalatao - add 10 durbadtavo WAV files (replace/split 5 parts). Convert to MP3, upload.
# Source: Desktop folder with 3.1, 3.2, 8.1, 8.2, 9.1, 9.2, 12.1, 12.2, 14.1, 14.2 WAV
# Run: cd f:\ss\audio-repo   python add_durbalatao_durbadtavo.py

# (release_mp3_name, source_wav_pattern, new_title)
# Maps to Durbalatao: 2.ભૂમિકા, 7.ગુરુવાદ, 8.બળવાન શરીર, 11.માનવ-ભગવાનવાદ, 27.ઉપસંહાર
FILE_MAP = [
    ("2.1.mp3", "3.1", "2.01 ભૂમિકા"),
    ("2.2.mp3", "3.2", "2.02 ભૂમિકા"),
    ("7.1.mp3", "8.1", "7.01 ગુરુવાદ કે બહુગુરુવાદ"),
    ("7.2.mp3", "8.2", "7.02 ગુરુવાદ કે બહુગુરુવાદ"),
    ("8.1.mp3", "9.1", "8.01 બળવાન શરીર"),
    ("8.2.mp3", "9.2", "8.02 બળવાન શરીર"),
    ("11.1.mp3", "12.1", "11.01 માનવ-ભગવાનવાદ"),
    ("11.2.mp3", "12.2", "11.02 માનવ-ભગવાનવાદ"),
    ("14.1.mp3", "14.1", "14.01 ઉપસંહાર"),
    ("14.2.mp3", "14.2", "14.02 ઉપસંહાર"),
]

import json
import subprocess
from pathlib import Path

REPO = "daveashish12356-dotcom/swamisachidanand-audio"
TAG = "durbalatao"
desktop = Path.home() / "Desktop"

def find_wav(folder, pattern):
    for p in folder.glob("*.wav"):
        if pattern in p.name:
            return p
    return None

# Find source folder (10 WAV - durbadtavo folder)
source_folder = None
for d in desktop.iterdir():
    if not d.is_dir():
        continue
    wavs = list(d.glob("*.wav"))
    if len(wavs) == 10 and any("3.1" in w.name or "ભૂમિકા" in w.name for w in wavs):
        source_folder = d
        break

if not source_folder:
    print("Source folder not found (10 WAV with 3.1/ભૂમિકા).")
    exit(1)

print("Found source,", len(list(source_folder.glob("*.wav"))), "WAV files")

upload_dir = Path(__file__).resolve().parent / "durbalatao_durbadtavo_mp3"
upload_dir.mkdir(exist_ok=True)
to_upload = []

for out_name, pattern, new_title in FILE_MAP:
    wav_path = find_wav(source_folder, pattern)
    if not wav_path or not wav_path.exists():
        print("Skip (not found):", pattern)
        continue
    mp3_path = upload_dir / out_name
    print("Converting ->", out_name)
    subprocess.run(
        ["ffmpeg", "-y", "-i", str(wav_path), "-codec:a", "libmp3lame", "-qscale:a", "2", str(mp3_path)],
        capture_output=True, check=False
    )
    if mp3_path.exists():
        to_upload.append(str(mp3_path))

if not to_upload:
    print("No MP3 created.")
    exit(1)

print("Uploading to durbalatao release...")
subprocess.run(["gh", "release", "upload", TAG] + to_upload + ["--repo", REPO], check=True)
print("Uploaded", len(to_upload), "files.")

# Update durbalatao_parts.json - replace single parts with split parts
base_url = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/durbalatao/"
parts_path = Path(__file__).resolve().parent / "durbalatao_parts.json"

with open(parts_path, encoding="utf-8-sig") as f:
    parts = json.load(f)

# Replace part index -> [(title, url), ...]  (split single part into 2)
replacements = {
    1: [("2.01 ભૂમિકા", base_url + "2.1.mp3"), ("2.02 ભૂમિકા", base_url + "2.2.mp3")],
    6: [("7.01 ગુરુવાદ કે બહુગુરુવાદ", base_url + "7.1.mp3"), ("7.02 ગુરુવાદ કે બહુગુરુવાદ", base_url + "7.2.mp3")],
    7: [("8.01 બળવાન શરીર", base_url + "8.1.mp3"), ("8.02 બળવાન શરીર", base_url + "8.2.mp3")],
    10: [("11.01 માનવ-ભગવાનવાદ", base_url + "11.1.mp3"), ("11.02 માનવ-ભગવાનવાદ", base_url + "11.2.mp3")],
    26: [("14.01 ઉપસંહાર", base_url + "14.1.mp3"), ("14.02 ઉપસંહાર", base_url + "14.2.mp3")],
}

new_parts = []
next_id = 1
for i, p in enumerate(parts):
    if i in replacements:
        for title, url in replacements[i]:
            new_parts.append({"id": str(next_id), "title": title, "url": url})
            next_id += 1
    else:
        p["id"] = str(next_id)
        new_parts.append(p)
        next_id += 1

with open(parts_path, "w", encoding="utf-8") as f:
    json.dump(new_parts, f, ensure_ascii=False, separators=(",", ":"))

print("Updated durbalatao_parts.json. Run rebuild_audio_list or add durbalatao to audio_list.")
