# -*- coding: utf-8 -*-
"""Build mahabharat_chintan 134 parts from Desktop folder. Part N = Nth file, title = filename, url = N.mp3"""
import json
import re
import shutil
from pathlib import Path

# Desktop folder: 'મહાભારત'નું ચિંતન (with quote in name)
SOURCE_DIR = Path(r"C:\Users\davea\Desktop") / "'મહાભારત'નું ચિંતન"
AUDIO_LIST = Path("public/audio_list.json")
FALLBACK = Path("app/src/main/assets/audio_list_fallback.json")
BASE = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/mahabharat_chintan/"


def sort_key(name):
    """Order: by leading number, then by (1), (2) if present."""
    base = Path(name).stem
    num = 0
    sub = 0
    m = re.match(r"^(\d+)", base)
    if m:
        num = int(m.group(1))
    bracket = re.search(r"\s*\((\d+)\)\s*$", base)
    if bracket:
        sub = int(bracket.group(1))
    return (num, sub, name)


def main():
    if not SOURCE_DIR.is_dir():
        raise SystemExit(f"Source folder not found: {SOURCE_DIR}")

    wavs = list(SOURCE_DIR.glob("*.wav"))
    if len(wavs) < 134:
        raise SystemExit(f"Expected at least 134 WAV files, found {len(wavs)}")

    sorted_files = sorted(wavs, key=lambda p: sort_key(p.name))
    selected = sorted_files[:134]

    parts = []
    for i, f in enumerate(selected, start=1):
        title = f.stem
        parts.append({
            "id": str(i),
            "title": title,
            "url": BASE + str(i) + ".mp3"
        })

    with open(AUDIO_LIST, encoding="utf-8-sig") as fp:
        data = json.load(fp)

    for book in data["books"]:
        if book["id"] == "mahabharat_chintan":
            book["parts"] = parts
            break

    with open(AUDIO_LIST, "w", encoding="utf-8") as fp:
        json.dump(data, fp, ensure_ascii=False, separators=(",", ":"))

    shutil.copy2(AUDIO_LIST, FALLBACK)
    print(f"Updated mahabharat_chintan with {len(parts)} parts from {SOURCE_DIR}")
    print(f"First: {parts[0]['title'][:50]}...")
    print(f"Last:  {parts[-1]['title'][:50]}...")


if __name__ == "__main__":
    main()
