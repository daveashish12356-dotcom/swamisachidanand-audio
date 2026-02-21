# Add/update આપણી દુર્બળતાઓ in audio_list. Run from f:\ss.
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PARTS_PATH = ROOT / "audio-repo" / "durbalatao_parts.json"
PATHS = [
    ROOT / "public" / "audio_list.json",
    ROOT / "audio_list.json",
    ROOT / "app" / "src" / "main" / "assets" / "audio_list_fallback.json",
    ROOT / "audio-repo" / "public" / "audio_list.json",
    ROOT / "audio-repo" / "audio_list.json",
]

def main():
    with open(PARTS_PATH, encoding="utf-8-sig") as f:
        parts = json.load(f)
    book = {"id": "aapni_durbalatao", "title": "આપણી દુર્બળતાઓ", "parts": parts}
    for path in PATHS:
        if not path.exists():
            continue
        with open(path, encoding="utf-8-sig") as f:
            data = json.load(f)
        books = data.get("books", [])
        for b in books:
            if b.get("id") == "aapni_durbalatao":
                b["parts"] = parts
                b["title"] = book["title"]
                break
        else:
            books.append(book)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
        print(f"Updated: {path}")
    print(f"Updated aapni_durbalatao with {len(parts)} parts.")

if __name__ == "__main__":
    main()
