# -*- coding: utf-8 -*-
"""Fix URLs for 'આપણે અને સમાજ' audio book in public/audio_list.json.

GitHub release assets are named with numeric prefixes only:
  1.mp3, 2.mp3, 3.-.mp3, 4.mp3, 5.mp3, 6.1.mp3, 6.2.mp3, ...

Current JSON still uses full Gujarati filenames (with spaces and title text).
This script rewrites URLs to match the actual asset filenames above.
"""
import json
import os
import re

REPO_ROOT = os.path.dirname(os.path.abspath(__file__))
AUDIO_LIST_JSON = os.path.join(REPO_ROOT, "public", "audio_list.json")
BOOK_ID = "aapne_ane_samaj"
BASE_PREFIX = (
    "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/"
    "releases/download/aapne_ane_samaj/"
)


def infer_filename(title: str) -> str | None:
    """Map part title to correct asset filename."""
    if not title:
        return None
    title = title.strip()

    # Special case: "3. તે સૌ-"
    if title.startswith("3.") and "તે સૌ" in title:
        return "3.-.mp3"

    # Match patterns like "6.1 ...", "10.5 ...", "1.  ...", "15. ..."
    m = re.match(r"^(\d+(?:\.\d+)?)", title)
    if not m:
        return None
    num = m.group(1)
    # For simple "1", "2", "4", "5", "15" → asset is "<num>.mp3"
    # For "6.1", "6.2", ... "14.5" → asset is "<num>.mp3"
    return f"{num}.mp3"


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
        for part in book.get("parts", []):
            title = part.get("title", "")
            fname = infer_filename(title)
            if not fname:
                continue
            part["url"] = BASE_PREFIX + fname
        break

    with open(AUDIO_LIST_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print("Fixed URLs for", BOOK_ID, "in", AUDIO_LIST_JSON)


if __name__ == "__main__":
    main()

