# Remove mari_bypass_surgery book from audio lists.
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PATHS = [
    ROOT / "public" / "audio_list.json",
    ROOT / "app" / "src" / "main" / "assets" / "audio_list_fallback.json",
]
GHP = Path("F:/ss-ghp/audio_list.json")

def main():
    for p in PATHS:
        if not p.exists():
            continue
        with open(p, encoding="utf-8-sig") as f:
            data = json.load(f)
        books = [b for b in data.get("books", []) if b.get("id") != "mari_bypass_surgery"]
        data["books"] = books
        with open(p, "w", encoding="utf-8") as f:
            if "fallback" in str(p):
                json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
            else:
                json.dump(data, f, ensure_ascii=False, indent=2)
        print("Removed mari_bypass_surgery from", p.name)
    if GHP.exists():
        with open(GHP, encoding="utf-8") as f:
            data = json.load(f)
        data["books"] = [b for b in data.get("books", []) if b.get("id") != "mari_bypass_surgery"]
        with open(GHP, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print("Removed mari_bypass_surgery from gh-pages audio_list.json")

if __name__ == "__main__":
    main()
