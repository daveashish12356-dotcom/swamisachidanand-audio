# -*- coding: utf-8 -*-
# Ramayan Chintan - rebuild from source. All WAV -> MP3, upload, update parts.
# Source: D:\New folder\New folder\રામાયણ'નું ચિતન
# Run: cd f:\ss\audio-repo   python rebuild_ramayan_chintan_from_source.py

import json
import subprocess
from pathlib import Path

REPO = "daveashish12356-dotcom/swamisachidanand-audio"
TAG = "ramayan_chintan"
# Primary path from user
SOURCE_PATH = Path(r"D:\New folder\New folder\રામાયણ'નું ચિતન")
BASE = Path(r"D:\New folder\New folder")

# Find Ramayan Chintan folder - MUST be Ramayan, NOT Mahabharat
def is_ramayan_folder(d):
    wavs = list(d.glob("*.wav"))
    if len(wavs) < 60 or len(wavs) > 85:
        return False
    # First file should be Ramayan (1.રામાયણ or ભૂમિકા), not Mahabharat
    for w in wavs[:5]:
        s = w.stem
        if "મહાભારત" in s or "ભીષ્મ" in s or "પાંડવ" in s:
            return False
        if "રામાયણ" in s or "ભૂમિકા" in s or "અર્પણ" in s:
            return True
    return False

source_folder = None
if SOURCE_PATH.exists() and SOURCE_PATH.is_dir():
    wavs = list(SOURCE_PATH.glob("*.wav"))
    if len(wavs) >= 60:
        source_folder = SOURCE_PATH
if not source_folder:
    for name in ['રામાયણ\'નું ચિતન', '"રામાયણ\'નું ચિતન"', 'રામાયણનું ચિતન']:
        p = BASE / name
        if p.exists() and is_ramayan_folder(p):
            source_folder = p
            break
if not source_folder:
    for d in BASE.iterdir():
        if not d.is_dir():
            continue
        if is_ramayan_folder(d):
            source_folder = d
            break

if not source_folder:
    print("Ramayan Chintan folder not found in", BASE)
    exit(1)

def sort_key(p):
    name = p.stem
    # 1.xxx, 2.xxx, 10.xxx, 22.xxx (1)
    parts = name.replace("(", " ").replace(")", " ").split()
    try:
        n1 = int(parts[0].split(".")[0])
    except:
        return (9999, name)
    n2 = 0
    if len(parts) > 1 and parts[-1].isdigit():
        n2 = int(parts[-1])
    return (n1 * 1000 + n2, name)

wavs = sorted(source_folder.glob("*.wav"), key=sort_key)
total = len(wavs)
print("Found", total, "WAV files in source")

if total < 1:
    print("No WAV files.")
    exit(1)

upload_dir = Path(__file__).resolve().parent / "ramayan_chintan_rebuild_mp3"
upload_dir.mkdir(exist_ok=True)

# Convert WAV -> MP3
for i, wav in enumerate(wavs):
    num = i + 1
    mp3 = upload_dir / f"{num}.mp3"
    print(f"[{num}/{total}] Converting...")
    subprocess.run(
        ["ffmpeg", "-y", "-i", str(wav), "-codec:a", "libmp3lame", "-qscale:a", "2", str(mp3)],
        capture_output=True, check=False
    )

mp3s = sorted(upload_dir.glob("*.mp3"), key=lambda p: int(p.stem))
if len(mp3s) < total:
    print("Only", len(mp3s), "MP3 created.")
    exit(1)

# Build parts from WAV stems
base_url = f"https://github.com/{REPO}/releases/download/{TAG}/"
parts = []
for i, wav in enumerate(wavs):
    num = i + 1
    title = wav.stem
    url = base_url + f"{num}.mp3"
    parts.append({"id": str(num), "title": title, "url": url})

# Save parts JSON
parts_path = Path(__file__).resolve().parent / "ramayan_chintan_parts.json"
with open(parts_path, "w", encoding="utf-8") as f:
    json.dump(parts, f, ensure_ascii=False, separators=(",", ":"))

# Upload to release
to_upload = [str(p) for p in mp3s]
print("Uploading", len(to_upload), "MP3 to ramayan_chintan release...")
subprocess.run(["gh", "release", "upload", TAG] + to_upload + ["--repo", REPO, "--clobber"], check=True)

print("Done. Parts:", len(parts))
print("Next: python add_ramayan_chintan_to_audio_list.py (from f:\\ss)")
