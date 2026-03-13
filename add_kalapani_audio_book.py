# -*- coding: utf-8 -*-
"""Add 'કલાપાણી' audio book: convert WAV->MP3, PDF->thumbnail, update audio_list.json"""
import json
import os
import re
import shutil
import subprocess
import sys
from datetime import datetime

AUDIO_FOLDER = r"C:\Users\davea\Desktop\કાલાપાની"
THUMBNAIL_PDF = r"C:\Users\davea\Desktop\કલાપાણી.pdf"
AUDIO_LIST_JSON = os.path.join(os.path.dirname(os.path.abspath(__file__)), "public", "audio_list.json")
PUBLIC_THUMBNAILS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "public", "thumbnails")

BOOK_ID = "kalapani"
BOOK_TITLE = "કલાપાણી"
RELEASE_TAG = "kalapani"
BASE_URL = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/"


def find_audio_files(folder_path):
    if not os.path.isdir(folder_path):
        return []
    audio_files = []
    for f in os.listdir(folder_path):
        full = os.path.join(folder_path, f)
        if os.path.isfile(full):
            ext = os.path.splitext(f)[1].lower()
            if ext in ['.wav', '.mp3', '.m4a']:
                audio_files.append(f)
    def sort_key(name):
        base = os.path.splitext(name)[0]
        nums = [int(n) for n in re.findall(r'\d+', base)]
        return (0, tuple(nums), base) if nums else (1, (), base)
    audio_files.sort(key=sort_key)
    return audio_files


def convert_wav_to_mp3(folder_path):
    """Convert all .wav in folder to .mp3 using ffmpeg, remove .wav after."""
    wavs = [f for f in os.listdir(folder_path) if os.path.splitext(f)[1].lower() == '.wav']
    if not wavs:
        return 0
    try:
        subprocess.run(["ffmpeg", "-version"], capture_output=True, check=True)
    except (FileNotFoundError, subprocess.CalledProcessError):
        print("ffmpeg not found. Install ffmpeg and add to PATH.")
        return -1
    converted = 0
    for f in wavs:
        src = os.path.join(folder_path, f)
        dest = os.path.join(folder_path, os.path.splitext(f)[0] + ".mp3")
        if os.path.isfile(dest):
            os.remove(src)
            converted += 1
            continue
        r = subprocess.run([
            "ffmpeg", "-y", "-i", src, "-acodec", "libmp3lame", "-q:a", "2", dest
        ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if r.returncode == 0 and os.path.isfile(dest):
            os.remove(src)
            converted += 1
            print("Converted:", converted)
    return converted


def copy_thumbnail(pdf_path, book_id):
    if not os.path.isfile(pdf_path):
        print("Warning: PDF not found:", pdf_path)
        return None
    dest_jpg = os.path.join(PUBLIC_THUMBNAILS, f"{book_id}.jpg")
    os.makedirs(PUBLIC_THUMBNAILS, exist_ok=True)
    try:
        import fitz
        doc = fitz.open(pdf_path)
        page = doc.load_page(0)
        pix = page.get_pixmap(dpi=150)
        pix.save(dest_jpg)
        print("Thumbnail:", dest_jpg)
        return f"{book_id}.jpg"
    except Exception as e:
        print("PyMuPDF failed:", e)
    try:
        from pdf2image import convert_from_path
        images = convert_from_path(pdf_path, first_page=1, last_page=1, dpi=150)
        if images:
            images[0].save(dest_jpg, 'JPEG', quality=85)
            print("Thumbnail:", dest_jpg)
            return f"{book_id}.jpg"
    except Exception as e:
        print("pdf2image failed:", e)
    return None


def create_parts(audio_files, release_tag):
    parts = []
    for i, filename in enumerate(audio_files, 1):
        base_name = os.path.splitext(filename)[0]
        url = f"{BASE_URL}{release_tag}/{i}.mp3"
        parts.append({"id": str(i), "title": base_name, "url": url})
    return parts


def add_book_to_json():
    if not os.path.isfile(AUDIO_LIST_JSON):
        print("Error: audio_list.json not found")
        return False
    with open(AUDIO_LIST_JSON, 'r', encoding='utf-8') as f:
        content = f.read()
        if content.startswith('\ufeff'):
            content = content[1:]
        data = json.loads(content)
    books = data.get('books', [])
    audio_files = find_audio_files(AUDIO_FOLDER)
    if not audio_files:
        print("Error: no audio files in", AUDIO_FOLDER)
        return False
    new_book = {
        "id": BOOK_ID,
        "title": BOOK_TITLE,
        "parts": create_parts(audio_files, RELEASE_TAG),
    }
    thumb_name = copy_thumbnail(THUMBNAIL_PDF, BOOK_ID)
    if thumb_name:
        new_book["thumbnailUrl"] = f"https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/thumbnails/{thumb_name}"
    for b in books:
        if b.get('id') == BOOK_ID:
            b['title'] = new_book['title']
            b['parts'] = new_book['parts']
            if thumb_name:
                b['thumbnailUrl'] = new_book['thumbnailUrl']
            break
    else:
        books.append(new_book)
    data['version'] = data.get('version', 1) + 1
    data['updated'] = datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")
    with open(AUDIO_LIST_JSON, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print("Updated", AUDIO_LIST_JSON)
    return True


def main():
    print("Converting WAV to MP3...")
    n = convert_wav_to_mp3(AUDIO_FOLDER)
    if n == -1:
        sys.exit(1)
    if n > 0:
        print("Converted", n, "files to MP3")
    audio_files = find_audio_files(AUDIO_FOLDER)
    print("Audio files:", len(audio_files))
    if not audio_files:
        print("No audio files found")
        sys.exit(1)
    copy_thumbnail(THUMBNAIL_PDF, BOOK_ID)
    if add_book_to_json():
        print("Done. Release tag:", RELEASE_TAG)
        print("Next: copy 1.mp3..%d.mp3 to audio-repo, then gh release upload" % len(audio_files))


if __name__ == "__main__":
    main()

