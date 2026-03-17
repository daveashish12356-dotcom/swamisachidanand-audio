# Server (public/audio_list.json) me HAR book ke parts 1, 2, 3 ... N order me lao.
# 1 ke bad 10,11,12,13,14,2,3... na aave – sirf server file fix, app build nahi chahiye.
# Run: python fix_audio_list_parts_order_server.py
# Phir isi public/audio_list.json ko swamisachidanand-audio repo (main) par push karo.
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PUBLIC_LIST = ROOT / "public" / "audio_list.json"


def part_sort_key(p):
    """Id numeric: 1, 2, 3, ... 10, 11, 14."""
    pid = (p or {}).get("id") or ""
    try:
        return (0, int(pid.strip()))
    except ValueError:
        return (1, pid)


def main():
    with open(PUBLIC_LIST, encoding="utf-8-sig") as f:
        data = json.load(f)
    books = data.get("books", [])
    fixed = 0
    for book in books:
        parts = book.get("parts")
        if not parts or len(parts) <= 1:
            continue
        sorted_parts = sorted(parts, key=part_sort_key)
        if sorted_parts != parts:
            book["parts"] = sorted_parts
            fixed += 1
            print(f"  Sorted parts for: {book.get('id', '?')} ({book.get('title', '')[:40]}...)")
    with open(PUBLIC_LIST, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"Done. Updated {PUBLIC_LIST} – {fixed} books parts ab 1,2,3...N order me. Server par push karo.")


if __name__ == "__main__":
    main()
