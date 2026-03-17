# Server (public/audio_list.json) me mari_bypass_surgery ka part list 1-14 order me, line sar (ek part ek line).
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PUBLIC_LIST = ROOT / "public" / "audio_list.json"
BASE_URL = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/mari_bypass_surgery"

def main():
    with open(PUBLIC_LIST, encoding="utf-8-sig") as f:
        data = json.load(f)
    books = data.get("books", [])
    # Parts 1 to 14, ek dam line sar: id, title ભાગ N, url
    parts = [
        {"id": str(i), "title": f"ભાગ {i}", "url": f"{BASE_URL}/{i}.mp3"}
        for i in range(1, 15)
    ]
    book = {"id": "mari_bypass_surgery", "title": "મારી બાયપાસ સર્જરી", "parts": parts}
    found = next((b for b in books if b.get("id") == "mari_bypass_surgery"), None)
    if found:
        found["parts"] = parts
        found["title"] = book["title"]
    else:
        books.append(book)
    # Line sar: indent=2 se har part alag line par
    with open(PUBLIC_LIST, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"Updated {PUBLIC_LIST}: mari_bypass_surgery parts 1-14, line sar.")

if __name__ == "__main__":
    main()
