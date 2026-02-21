# -*- coding: utf-8 -*-
# Fix geetaji_chintan parts order: (1),(10),(11)...(2) -> (1),(2),(3)...(21)
# Run: cd f:\ss\audio-repo  python fix_geetaji_parts_order.py

import json
import re
from pathlib import Path

PARTS = Path(__file__).resolve().parent / "geetaji_chintan_parts.json"

def extract_sort_key(part):
    """(section, subsection, title). Parse X.Y or X. from title."""
    title = part.get("title", "")
    m = re.match(r'^(\d+)\.(\d+)\s', title)
    if m:
        return (int(m.group(1)), int(m.group(2)), title)
    m = re.match(r'^(\d+)\.', title)
    return (int(m.group(1)), 0, title) if m else (0, 0, title)

def main():
    data = json.load(open(PARTS, encoding="utf-8-sig"))
    sorted_parts = sorted(data, key=extract_sort_key)
    with open(PARTS, "w", encoding="utf-8") as f:
        json.dump(sorted_parts, f, ensure_ascii=False, indent=2)
    print("Sorted geetaji_chintan_parts.json by numeric (N) order.")

if __name__ == "__main__":
    main()
