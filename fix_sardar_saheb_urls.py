# -*- coding: utf-8 -*-
"""Fix URLs for 'સરદારસાહેબ મારી નજરે' audio book in public/audio_list.json.

Release assets are named 1.1.mp3, 1.2.mp3, 2.1.mp3, ..., 2.17.mp3
but current JSON still points to filenames that include Gujarati title text.
This script rewrites URLs to match the numeric asset filenames.
"""
import json
import os
import re

REPO_ROOT = os.path.dirname(os.path.abspath(__file__))
AUDIO_LIST_JSON = os.path.join(REPO_ROOT, "public", "audio_list.json")
BOOK_ID = "sardar_saheb_mari_najare"
BASE_URL_PREFIX = (
    "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/"
    "releases/download/sardar_saheb_mari_najare/"
)


def main():
    with open(AUDIO_LIST_JSON, "r", encoding="utf-8") as f:
        content = f.read()
        if content.startswith("\ufeff"):
            content = content[1:]
        data = json.loads(content)

    books = data.get("books", [])
    for book in books:
        if book.get("id") != BOOK_ID:
            continue
        parts = book.get("parts", [])
        for p in parts:
            title = p.get("title", "")
            # Extract leading number pattern like "1.1" or "2.17"
            m = re.match(r"^(\d+\.\d+)", title)
            if not m:
                continue
            fname = m.group(1) + ".mp3"
            p["url"] = BASE_URL_PREFIX + fname
        break

    with open(AUDIO_LIST_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print("Fixed URLs for", BOOK_ID, "in", AUDIO_LIST_JSON)


if __name__ == "__main__":
    main()

