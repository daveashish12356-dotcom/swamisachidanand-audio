# Desktop folder part names: 1.મારી બાયપાસ સર્જરી (1) ... 14.મારી બાયપાસ સર્જરી (14). Book name same.
# Updates public/audio_list.json and optionally gh-pages.
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PUBLIC = ROOT / "public" / "audio_list.json"
BASE = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/mari_bypass_surgery"

# Tumhare hisab na part titles (name ye hi rakhna)
TITLES = [
    "1.મારી બાયપાસ સર્જરી (1)",
    "2.મારી બાયપાસ સર્જરી (2)",
    "3.મારી બાયપાસ સર્જરી (3)",
    "4.મારી બાયપાસ સર્જરી (4)",
    "5.મારી બાયપાસ સર્જરી (5)",
    "6.મારી બાયપાસ સર્જરી (6)",
    "7.મારી બાયપાસ સર્જરી (7)",
    "8.મારી બાયપાસ સર્જરી (8)",
    "9.મારી બાયપાસ સર્જરી (9)",
    "10.મારી બાયપાસ સર્જરી (10)",
    "11.મારી બાયપાસ સર્જરી (11)",
    "12.મારી બાયપાસ સર્જરી (12)",
    "13.મારી બાયપાસ સર્જરી (13)",
    "14.મારી બાયપાસ સર્જરી (14)",
]

def main():
    parts = [
        {"id": str(i), "title": TITLES[i - 1], "url": f"{BASE}/{i}.mp3"}
        for i in range(1, 15)
    ]
    book = {"id": "mari_bypass_surgery", "title": "મારી બાયપાસ સર્જરી", "parts": parts}

    with open(PUBLIC, encoding="utf-8") as f:
        data = json.load(f)
    books = data.get("books", [])
    for b in books:
        if b.get("id") == "mari_bypass_surgery":
            b["title"] = book["title"]
            b["parts"] = parts
            break
    else:
        books.append(book)

    with open(PUBLIC, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print("Updated public/audio_list.json: mari_bypass_surgery with your part names (1-14).")

    ghp = Path("F:/ss-ghp/audio_list.json")
    if ghp.exists():
        with open(ghp, encoding="utf-8") as f:
            data2 = json.load(f)
        books2 = data2.get("books", [])
        for b in books2:
            if b.get("id") == "mari_bypass_surgery":
                b["title"] = book["title"]
                b["parts"] = parts
                break
        else:
            books2.append(book)
        with open(ghp, "w", encoding="utf-8") as f:
            json.dump(data2, f, ensure_ascii=False, indent=2)
        print("Updated F:/ss-ghp/audio_list.json (gh-pages).")

if __name__ == "__main__":
    main()
