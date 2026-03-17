# -*- coding: utf-8 -*-
"""Create GitHub release for 'સરદારસાહેબ મારી નજરે' audio MP3s.

This script avoids Windows shell encoding issues by calling `gh` directly
from Python with full Unicode paths.
"""
import os
import re
import subprocess
import sys

DESKTOP_FOLDER = r"C:\Users\davea\Desktop\સરદારસાહેબ મારી નજરે"
REPO = "daveashish12356-dotcom/swamisachidanand-audio"
RELEASE_TAG = "sardar_saheb_mari_najare"
TITLE = "સરદારસાહેબ મારી નજરે"


def find_mp3_files(folder: str):
    if not os.path.isdir(folder):
        raise SystemExit(f"Audio folder not found: {folder}")
    files = [
        os.path.join(folder, f)
        for f in os.listdir(folder)
        if os.path.isfile(os.path.join(folder, f)) and f.lower().endswith(".mp3")
    ]

    def sort_key(path: str):
        name = os.path.splitext(os.path.basename(path))[0]
        nums = [int(n) for n in re.findall(r"\d+", name)]
        return (tuple(nums), name) if nums else ((9999,), name)

    files.sort(key=sort_key)
    return files


def ensure_gh():
    try:
        subprocess.run(["gh", "--version"], capture_output=True, check=True)
    except (FileNotFoundError, subprocess.CalledProcessError):
        raise SystemExit("GitHub CLI `gh` not found. Install it and login with `gh auth login`.")


def main():
    # Make stdout UTF-8 to avoid encode errors
    if sys.stdout.encoding.lower() != "utf-8":
        import io

        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

    mp3_files = find_mp3_files(DESKTOP_FOLDER)
    if not mp3_files:
        raise SystemExit("No MP3 files found in desktop folder.")

    print("Found MP3 files (in order):")
    for i, p in enumerate(mp3_files, 1):
        print(f"{i:2d}. {os.path.basename(p)}")

    ensure_gh()

    cmd = ["gh", "release", "create", RELEASE_TAG] + mp3_files + [
        "--title",
        TITLE,
        "--repo",
        REPO,
    ]
    print("\nRunning:", " ".join(cmd[:5]), "... <paths omitted>")
    result = subprocess.run(cmd)
    if result.returncode != 0:
        raise SystemExit(f"`gh release create` failed with code {result.returncode}")

    print("\n✓ GitHub release created:", RELEASE_TAG)


if __name__ == "__main__":
    main()

