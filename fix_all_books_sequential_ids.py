# -*- coding: utf-8 -*-
# All books: part ids = 1,2,3...N (sequential) so app sort by id gives correct order.
# Run: cd f:\ss   python audio-repo/fix_all_books_sequential_ids.py

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PATHS = [
    ROOT / "public" / "audio_list.json",
    ROOT / "audio_list.json",
    ROOT / "app" / "src" / "main" / "assets" / "audio_list_fallback.json",
    ROOT / "audio-repo" / "public" / "audio_list.json",
    ROOT / "audio-repo" / "audio_list.json",
]
GHP = ROOT.parent / "ss-ghp" / "audio_list.json"
if GHP.exists():
    PATHS.append(GHP)


def main():
    for p in PATHS:
        if not p.exists():
            continue
        data = json.load(open(p, encoding="utf-8-sig"))
        books = data.get("books", [])
        for b in books:
            parts = b.get("parts", [])
            for i, part in enumerate(parts):
                part["id"] = str(i + 1)
        with open(p, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
        print(f"Updated: {p}  ({len(books)} books)")
    print("Done. All parts now have ids 1,2,3...N.")


if __name__ == "__main__":
    main()
