# -*- coding: utf-8 -*-
# Update main branch's audio_list.json with mahabharat_chintan parts.
# Run: python update_main_audio_mahabharat.py
# Then: git checkout main (after resolving conflicts), replace public/audio_list.json, commit, push

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
MAIN_AUDIO = ROOT / "main_audio_list.json"
PARTS_JSON = ROOT / "audio-repo" / "mahabharat_chintan_parts.json"
OUT = ROOT / "public" / "audio_list_main_updated.json"

# Git on Windows may output UTF-16
try:
    with open(MAIN_AUDIO, encoding="utf-16") as f:
        data = json.load(f)
except UnicodeDecodeError:
    with open(MAIN_AUDIO, encoding="utf-8-sig") as f:
        data = json.load(f)

with open(PARTS_JSON, encoding="utf-8-sig") as f:
    parts = json.load(f)

for b in data.get("books", []):
    if b.get("id") == "mahabharat_chintan":
        b["parts"] = parts
        print("Updated mahabharat_chintan with", len(parts), "parts")
        break
else:
    data.setdefault("books", []).append({
        "id": "mahabharat_chintan",
        "title": "મહાભારતનું ચિંતન",
        "parts": parts,
    })
    print("Added mahabharat_chintan with", len(parts), "parts")

from datetime import datetime, timezone
data["updated"] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

OUT.parent.mkdir(parents=True, exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

print("Wrote:", OUT)
print("\nNext: Copy to main branch's public/audio_list.json and push to origin main")
