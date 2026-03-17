# -*- coding: utf-8 -*-
"""Find all audio parts with duration 10 min 55 sec (655 sec) from server audio_list.json"""
import json
import subprocess
import sys
import os
from concurrent.futures import ThreadPoolExecutor, as_completed

# 10 min 55 sec = 655 seconds; allow small tolerance (654.5 - 655.5)
TARGET_SEC = 655
TOLERANCE = 0.5
MAX_WORKERS = 8

def get_duration_ffprobe(url):
    """Get duration in seconds using ffprobe. Returns None on error."""
    try:
        cmd = [
            "ffprobe", "-v", "error", "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1", "-i", url
        ]
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=45)
        if r.returncode == 0 and r.stdout.strip():
            return float(r.stdout.strip())
    except Exception:
        pass
    return None

def check_part(item):
    book_id, book_title, part, url = item
    dur = get_duration_ffprobe(url)
    return (book_id, book_title, part, dur) if dur else None

def main():
    path = os.path.join(os.path.dirname(__file__), "audio-repo", "public", "audio_list.json")
    if not os.path.exists(path):
        path = os.path.join(os.path.dirname(__file__), "audio_list.json")
    if not os.path.exists(path):
        print("audio_list.json not found")
        sys.exit(1)

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    tasks = []
    for book in data.get("books", []):
        book_id = book.get("id", "")
        book_title = book.get("title", "")
        for part in book.get("parts", []):
            url = part.get("url", "")
            if not url:
                continue
            tasks.append((book_id, book_title, part, url))

    results = []
    done = 0
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as ex:
        futures = {ex.submit(check_part, t): t for t in tasks}
        for fut in as_completed(futures):
            done += 1
            if done % 100 == 0:
                print(f"Checked {done}/{len(tasks)} parts...", file=sys.stderr)
            res = fut.result()
            if res:
                book_id, book_title, part, dur = res
                if abs(dur - TARGET_SEC) <= TOLERANCE:
                    results.append({
                        "book_id": book_id,
                        "book_title": book_title,
                        "part_title": part.get("title", ""),
                        "part_id": part.get("id", ""),
                        "duration_sec": dur,
                        "url": part.get("url", "")
                    })

    out = []
    out.append("=== 10 min 55 sec (655 sec) parts ===\n")
    if not results:
        out.append("No parts found with duration 10 min 55 sec.")
    else:
        for r in results:
            out.append(f"Part: {r['part_title']}")
            out.append(f"Book: {r['book_title']} ({r['book_id']})")
            out.append(f"Duration: {r['duration_sec']:.1f} sec")
            out.append("")
    text = "\n".join(out)
    outpath = os.path.join(os.path.dirname(__file__), "find_655_output.txt")
    with open(outpath, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"Found {len(results)} parts. Output written to find_655_output.txt")

if __name__ == "__main__":
    main()
