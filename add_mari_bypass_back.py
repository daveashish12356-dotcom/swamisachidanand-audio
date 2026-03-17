# Add મારી બાયપાસ સર્જરી back: audio_list (public, fallback, gh-pages) with thumbnail.
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PARTS_FILE = ROOT / "audio-repo" / "mari_bypass_parts.json"
THUMB_URL = "https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/main/thumbnails/mari_bypass_surgery.jpg"

PATHS = [
    ROOT / "public" / "audio_list.json",
    ROOT / "app" / "src" / "main" / "assets" / "audio_list_fallback.json",
]
GHP = Path("F:/ss-ghp/audio_list.json")

def main():
    with open(PARTS_FILE, encoding="utf-8-sig") as f:
        parts = json.load(f)
    book = {
        "id": "mari_bypass_surgery",
        "title": "મારી બાયપાસ સર્જરી",
        "thumbnailUrl": THUMB_URL,
        "parts": parts,
    }
    for p in PATHS:
        if not p.exists():
            continue
        with open(p, encoding="utf-8-sig") as f:
            data = json.load(f)
        books = data.get("books", [])
        if not any(b.get("id") == "mari_bypass_surgery" for b in books):
            books.append(book)
        else:
            for b in books:
                if b.get("id") == "mari_bypass_surgery":
                    b.update(book)
                    break
        with open(p, "w", encoding="utf-8") as f:
            if "fallback" in str(p):
                json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
            else:
                json.dump(data, f, ensure_ascii=False, indent=2)
        print("Updated:", p.name)
    if GHP.exists():
        with open(GHP, encoding="utf-8") as f:
            data = json.load(f)
        books = data.get("books", [])
        if not any(b.get("id") == "mari_bypass_surgery" for b in books):
            books.append(book)
        else:
            for b in books:
                if b.get("id") == "mari_bypass_surgery":
                    b.update(book)
                    break
        with open(GHP, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print("Updated: gh-pages audio_list.json")

if __name__ == "__main__":
    main()
