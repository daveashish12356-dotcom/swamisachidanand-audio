#! /usr/bin/env python3
# -*- coding: utf-8 -*-
"""Complete setup for ચાણક્યની રાજનીતિ: WAV→MP3, PDF→JPG, JSON, Release, Push."""

import json
import os
import shutil
import subprocess
import sys
from datetime import datetime

# Ensure UTF-8 stdout
if sys.stdout.encoding != "utf-8":
    import io

    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

# Paths (adjusted for ચાણક્યની રાજનીતિ)
REPO_ROOT = os.path.dirname(os.path.abspath(__file__))
AUDIO_FOLDER = r"C:\Users\davea\Desktop\ચાણક્યની રાજનીતિ"
THUMBNAIL_PDF = r"C:\Users\davea\Desktop\ચાણક્યની રાજનીતિ.pdf"
AUDIO_LIST_JSON = os.path.join(REPO_ROOT, "public", "audio_list.json")
THUMBNAIL_DEST = os.path.join(REPO_ROOT, "public", "thumbnails", "chanakya_rajniti.jpg")
MP3_DIR = os.path.join(REPO_ROOT, "chanakya_rajniti_mp3")

# Book details
BOOK_ID = "chanakya_rajniti"
BOOK_TITLE = "ચાણક્યની રાજનીતિ"
RELEASE_TAG = "chanakya_rajniti"
GITHUB_REPO = "daveashish12356-dotcom/swamisachidanand-audio"
BASE_URL = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download"
THUMBNAIL_URL = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/thumbnails/chanakya_rajniti.jpg"


def remove_old_entry() -> None:
    """Remove old ચાણક્યની રાજનીતિ audio entry from JSON if present."""
    if not os.path.isfile(AUDIO_LIST_JSON):
        return
    with open(AUDIO_LIST_JSON, "r", encoding="utf-8") as f:
        content = f.read()
        if content.startswith("\ufeff"):
            content = content[1:]
        data = json.loads(content)
    books = data.get("books", [])
    before = len(books)
    books = [b for b in books if b.get("id") != BOOK_ID]
    if len(books) < before:
        data["books"] = books
        with open(AUDIO_LIST_JSON, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"✓ Removed old {BOOK_ID} entry from audio_list.json")


def find_wav_files():
    """Find and sort WAV files in the Desktop folder."""
    if not os.path.isdir(AUDIO_FOLDER):
        print(f"ERROR: Folder not found: {AUDIO_FOLDER}")
        sys.exit(1)
    files = [
        f
        for f in os.listdir(AUDIO_FOLDER)
        if os.path.isfile(os.path.join(AUDIO_FOLDER, f)) and f.lower().endswith(".wav")
    ]
    if not files:
        print("ERROR: No WAV files found")
        sys.exit(1)
    import re

    def sort_key(name: str) -> int:
        nums = re.findall(r"\d+", name)
        return int(nums[0]) if nums else 999

    files.sort(key=sort_key)
    return files


def convert_wav_to_mp3(wav_files):
    """Convert all WAV files to numbered MP3 files (1.mp3, 2.mp3, ...)."""
    print("\n" + "=" * 60)
    print("Converting WAV to MP3...")
    print("=" * 60)

    try:
        subprocess.run(["ffmpeg", "-version"], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("ERROR: ffmpeg not found. Install with: winget install ffmpeg")
        sys.exit(1)

    os.makedirs(MP3_DIR, exist_ok=True)
    for f in os.listdir(MP3_DIR):
        if f.lower().endswith(".mp3"):
            os.remove(os.path.join(MP3_DIR, f))

    total = len(wav_files)
    for idx, name in enumerate(wav_files, 1):
        src = os.path.join(AUDIO_FOLDER, name)
        dest = os.path.join(MP3_DIR, f"{idx}.mp3")
        print(f"[{idx}/{total}] {name} -> {idx}.mp3 ...", end=" ")
        result = subprocess.run(
            ["ffmpeg", "-y", "-i", src, "-codec:a", "libmp3lame", "-qscale:a", "2", dest],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        if os.path.isfile(dest):
            print("✓")
        else:
            print("✗ FAILED")
            print(f"  ffmpeg exit code: {result.returncode}")

    mp3_count = len([f for f in os.listdir(MP3_DIR) if f.lower().endswith(".mp3")])
    if mp3_count != total:
        print(f"\nERROR: Only {mp3_count}/{total} MP3 files created")
        sys.exit(1)
    print(f"\n✓ Converted {mp3_count} files to MP3")
    return mp3_count


def create_thumbnail() -> bool:
    """Create JPG thumbnail from first page of Desktop PDF."""
    print("\n" + "=" * 60)
    print("Creating thumbnail from PDF...")
    print("=" * 60)

    if not os.path.isfile(THUMBNAIL_PDF):
        print(f"ERROR: PDF not found: {THUMBNAIL_PDF}")
        return False

    os.makedirs(os.path.dirname(THUMBNAIL_DEST), exist_ok=True)
    try:
        from pdf2image import convert_from_path

        images = convert_from_path(THUMBNAIL_PDF, first_page=1, last_page=1, dpi=150)
        if images:
            images[0].save(THUMBNAIL_DEST, "JPEG", quality=85)
            print(f"✓ Thumbnail created: {THUMBNAIL_DEST}")
            return True
    except ImportError:
        print("WARNING: pdf2image not installed (pip install pdf2image pillow).")
    except Exception as e:
        print(f"ERROR creating thumbnail: {e}")

    print("⚠ Thumbnail not created automatically. Create from PDF first page manually if needed.")
    return False


def update_audio_json(wav_files):
    """Add/update ચાણક્યની રાજનીતિ entry in audio_list.json with MP3 parts."""
    print("\n" + "=" * 60)
    print("Updating audio_list.json...")
    print("=" * 60)

    if not os.path.isfile(AUDIO_LIST_JSON):
        print(f"ERROR: audio_list.json not found at {AUDIO_LIST_JSON}")
        sys.exit(1)

    with open(AUDIO_LIST_JSON, "r", encoding="utf-8") as f:
        content = f.read()
        if content.startswith("\ufeff"):
            content = content[1:]
        data = json.loads(content)

    parts = []
    for idx, name in enumerate(wav_files, 1):
        base = os.path.splitext(name)[0]
        parts.append(
            {
                "id": str(idx),
                "title": base,
                "url": f"{BASE_URL}/{RELEASE_TAG}/{idx}.mp3",
            }
        )

    books = data.get("books", [])
    books = [b for b in books if b.get("id") != BOOK_ID]
    new_book = {
        "id": BOOK_ID,
        "title": BOOK_TITLE,
        "thumbnailUrl": THUMBNAIL_URL,
        "parts": parts,
        # Mark as new so Home "નવાં ઓડિયો" section highlights it initially.
        "new": True,
        # Category uses common "granth" bucket (મહત્વના ગ્રંથો).
        "category": "granth",
    }
    books.append(new_book)
    data["books"] = books
    data["version"] = data.get("version", 1) + 1
    data["updated"] = datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")

    with open(AUDIO_LIST_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"✓ audio_list.json updated with {len(parts)} parts for {BOOK_TITLE}")


def create_github_release():
    """Create GitHub release with numbered MP3s using gh CLI (best-effort)."""
    print("\n" + "=" * 60)
    print("Creating GitHub release (best-effort)...")
    print("=" * 60)

    mp3_files = [f for f in os.listdir(MP3_DIR) if f.lower().endswith(".mp3")]
    if not mp3_files:
        print("WARNING: No MP3 files found to attach to release.")
        return False

    mp3_files.sort(key=lambda x: int(os.path.splitext(x)[0]))
    mp3_paths = [os.path.join(MP3_DIR, f) for f in mp3_files]

    try:
        subprocess.run(["gh", "--version"], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("WARNING: GitHub CLI (gh) not found or not configured.")
        print("Create release manually if needed:")
        print(f"  https://github.com/{GITHUB_REPO}/releases/new")
        print(f"  Tag: {RELEASE_TAG}")
        print(f"  Upload MP3 files from: {MP3_DIR}")
        return False

    cmd = ["gh", "release", "create", RELEASE_TAG] + mp3_paths + [
        "--repo",
        GITHUB_REPO,
        "--title",
        BOOK_TITLE,
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode == 0:
        print("✓ GitHub release created")
        return True
    print("⚠ Release creation failed; create manually if needed.")
    print(result.stderr)
    return False


def main():
    print("=" * 60)
    print("ચાણક્યની રાજનીતિ – Complete server/audio setup")
    print("=" * 60)

    print("\n[1/6] Removing any old JSON entry...")
    remove_old_entry()

    print("\n[2/6] Finding WAV files in Desktop folder...")
    wav_files = find_wav_files()
    print(f"✓ Found {len(wav_files)} WAV files")

    print("\n[3/6] Converting WAV → MP3...")
    mp3_count = convert_wav_to_mp3(wav_files)

    print("\n[4/6] Creating thumbnail JPG from PDF...")
    thumb_ok = create_thumbnail()

    print("\n[5/6] Updating audio_list.json...")
    update_audio_json(wav_files)

    print("\n[6/6] Creating GitHub release (optional)...")
    release_ok = create_github_release()

    print("\nSUMMARY")
    print("=" * 60)
    print(f"MP3 files: {mp3_count} in {MP3_DIR}")
    print(f"Thumbnail: {'OK' if thumb_ok else 'MANUAL TODO'} -> {THUMBNAIL_DEST}")
    print(f"JSON: audio_list.json updated for {BOOK_TITLE}")
    print(f"Release: {'created' if release_ok else 'create manually if needed'}")
    print("\n✓ Setup script finished.")


if __name__ == "__main__":
    main()

