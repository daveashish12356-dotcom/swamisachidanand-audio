# -*- coding: utf-8 -*-
"""Complete setup for Chanakya Vyavaharniti: prepare files, check JSON, create release guide"""
import os
import shutil
import json
import sys

# Set UTF-8 for output
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

AUDIO_FOLDER = r"C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ"
THUMBNAIL_PDF = r"C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ.pdf"
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "chanakya_upload")
AUDIO_LIST_JSON = os.path.join(os.path.dirname(__file__), "public", "audio_list.json")
THUMBNAIL_DEST = os.path.join(os.path.dirname(__file__), "public", "thumbnails", "chanakya_vyavaharniti.jpg")

print("=" * 60)
print("Chanakya Vyavaharniti - Complete Setup")
print("=" * 60)

# 1. Check audio files
print("\n1. Checking audio files...")
if not os.path.isdir(AUDIO_FOLDER):
    print(f"   ERROR: Folder not found: {AUDIO_FOLDER}")
    sys.exit(1)

wav_files = [f for f in os.listdir(AUDIO_FOLDER) 
              if os.path.isfile(os.path.join(AUDIO_FOLDER, f)) and f.lower().endswith('.wav')]

if not wav_files:
    print("   ERROR: No WAV files found")
    sys.exit(1)

# Sort
import re
def sort_key(name):
    nums = re.findall(r'\d+', name)
    return int(nums[0]) if nums else 999
wav_files.sort(key=sort_key)

print(f"   ✓ Found {len(wav_files)} WAV files")

# 2. Prepare upload directory
print("\n2. Preparing upload directory...")
os.makedirs(UPLOAD_DIR, exist_ok=True)

# Clear old files
for f in os.listdir(UPLOAD_DIR):
    if f.endswith('.wav'):
        os.remove(os.path.join(UPLOAD_DIR, f))

# Copy with simple names
for i, filename in enumerate(wav_files, 1):
    src = os.path.join(AUDIO_FOLDER, filename)
    dest = os.path.join(UPLOAD_DIR, f"{i}.wav")
    shutil.copy2(src, dest)

copied = len([f for f in os.listdir(UPLOAD_DIR) if f.endswith('.wav')])
print(f"   ✓ Copied {copied} files to: {UPLOAD_DIR}")

# 3. Check JSON
print("\n3. Checking audio_list.json...")
if os.path.isfile(AUDIO_LIST_JSON):
    with open(AUDIO_LIST_JSON, 'r', encoding='utf-8') as f:
        content = f.read()
        if content.startswith('\ufeff'):
            content = content[1:]
        data = json.loads(content)
    
    book_found = False
    for book in data.get('books', []):
        if book.get('id') == 'chanakya_vyavaharniti':
            book_found = True
            parts_count = len(book.get('parts', []))
            has_thumb = 'thumbnailUrl' in book and book['thumbnailUrl']
            print(f"   ✓ Book found: {parts_count} parts")
            print(f"   ✓ Thumbnail URL: {'SET' if has_thumb else 'MISSING'}")
            break
    
    if not book_found:
        print("   ERROR: Book not found in JSON")
else:
    print("   ERROR: audio_list.json not found")

# 4. Check thumbnail
print("\n4. Checking thumbnail...")
if os.path.isfile(THUMBNAIL_DEST):
    size = os.path.getsize(THUMBNAIL_DEST)
    print(f"   ✓ Thumbnail exists: {size} bytes")
else:
    print(f"   ⚠ Thumbnail missing: {THUMBNAIL_DEST}")
    if os.path.isfile(THUMBNAIL_PDF):
        print(f"   → PDF found: {THUMBNAIL_PDF}")
        print("   → Extract first page as JPG and save to thumbnail path")

# 5. Summary and next steps
print("\n" + "=" * 60)
print("SUMMARY")
print("=" * 60)
print(f"Audio files: {copied} ready in {UPLOAD_DIR}")
print(f"JSON: Book entry {'found' if book_found else 'NOT FOUND'}")
print(f"Thumbnail: {'Ready' if os.path.isfile(THUMBNAIL_DEST) else 'NEEDED'}")

print("\n" + "=" * 60)
print("NEXT STEPS")
print("=" * 60)
print("\n1. CREATE THUMBNAIL (if not done):")
print(f"   Extract first page from: {THUMBNAIL_PDF}")
print(f"   Save as: {THUMBNAIL_DEST}")

print("\n2. CREATE GITHUB RELEASE:")
print(f"   cd {UPLOAD_DIR}")
print(f"   gh release create chanakya_vyavaharniti *.wav --repo daveashish12356-dotcom/swamisachidanand-audio --title 'Chanakya Vyavaharniti'")
print("\n   OR via browser:")
print("   https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/new")
print("   Tag: chanakya_vyavaharniti")
print(f"   Upload {copied} WAV files from: {UPLOAD_DIR}")

print("\n3. PUSH TO GITHUB:")
print("   git add public/audio_list.json")
if os.path.isfile(THUMBNAIL_DEST):
    print("   git add public/thumbnails/chanakya_vyavaharniti.jpg")
print("   git commit -m 'Add Chanakya Vyavaharniti audio book'")
print("   git push")

print("\n4. VERIFY IN APP:")
print("   - Open app → Audio tab")
print("   - Check if book appears with thumbnail")
print("   - Test audio playback")

print("\n" + "=" * 60)
