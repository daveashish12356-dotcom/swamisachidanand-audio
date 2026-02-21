# -*- coding: utf-8 -*-
# All books: "X.name (N)" -> "X.0N name" (like geetaji - bracket number after dot, padded)
# Run: cd f:\ss   python audio-repo/fix_all_books_title_format.py

import json
import re
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


def transform_title(title):
    """'5.name (1)' -> '5.01 name', '5.name (10)' -> '5.10 name'"""
    m = re.match(r'^(\d+)\.(.+?)\s*\((\d+)\)\s*(.*)$', title)
    if m:
        lead, name, num, rest = m.groups()
        n = int(num)
        padded = str(n).zfill(2)  # 1->01, 9->09, 10->10
        return f"{lead}.{padded} {name.rstrip()}{rest}".rstrip()
    return title


def main():
    total = 0
    for p in PATHS:
        if not p.exists():
            continue
        data = json.load(open(p, encoding="utf-8-sig"))
        count = 0
        for b in data.get("books", []):
            for part in b.get("parts", []):
                old = part.get("title", "")
                new = transform_title(old)
                if old != new:
                    part["title"] = new
                    count += 1
        if count:
            with open(p, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
            print(f"Updated {p.name}: {count} titles")
            total += count
    print(f"Done. Transformed {total} titles across all books.")


if __name__ == "__main__":
    main()
