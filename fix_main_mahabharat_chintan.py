# -*- coding: utf-8 -*-
"""Update mahabharat_chintan in main's audio_list - no encoding corruption.
Run from f:\ss. Reads from git, updates, writes. Then: git add, commit, push on main."""
import json
import subprocess
from pathlib import Path
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parent

# Get main's audio_list directly from git (binary, UTF-8)
r = subprocess.run(
    ["git", "show", "origin/main:public/audio_list.json"],
    capture_output=True,
    cwd=ROOT,
)
if r.returncode != 0:
    raise SystemExit("git show failed")
raw = r.stdout
# Try UTF-8 first (standard for JSON in git)
data = json.loads(raw.decode("utf-8"))

# Load mahabharat_chintan parts
parts_path = ROOT / "audio-repo" / "mahabharat_chintan_parts.json"
with open(parts_path, encoding="utf-8") as f:
    parts = json.load(f)

# Update mahabharat_chintan
for b in data.get("books", []):
    if b.get("id") == "mahabharat_chintan":
        b["parts"] = parts
        break
else:
    data.setdefault("books", []).append({
        "id": "mahabharat_chintan",
        "title": "મહાભારતનું ચિંતન",
        "parts": parts,
    })

data["updated"] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

out_path = ROOT / "public" / "audio_list.json"
with open(out_path, "w", encoding="utf-8", newline="\n") as f:
    json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

# Verify - read back and check first book title
with open(out_path, encoding="utf-8") as f:
    check = json.load(f)
first_title = check["books"][0]["title"]
if "અમર" in first_title or "amar" in first_title.lower():
    print("OK - encoding correct. First book:", first_title[:50])
else:
    raise SystemExit("Encoding check failed - first title: " + repr(first_title[:80]))

print("Updated public/audio_list.json with", len(parts), "mahabharat_chintan parts")
print("Next: git add public/audio_list.json && git commit -m 'Update mahabharat_chintan parts' && git push origin main")
