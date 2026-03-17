# -*- coding: utf-8 -*-
"""Prepare Chanakya Vyavaharniti for GitHub release"""
import os
import shutil
import json
from datetime import datetime

DESKTOP = os.path.join(os.path.expanduser('~'), 'Desktop')
SOURCE_FOLDER = None
for item in os.listdir(DESKTOP):
    full_path = os.path.join(DESKTOP, item)
    if os.path.isdir(full_path) and 'ચાણક્ય' in item:
        SOURCE_FOLDER = full_path
        break

if not SOURCE_FOLDER or not os.path.isdir(SOURCE_FOLDER):
    print("Error: Source folder not found on Desktop")
    exit(1)

TAG_NAME = "chanakya_vyavaharniti"
REPO = "daveashish12356-dotcom/swamisachidanand-audio"
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "chanakya_upload")

# Find WAV files
wav_files = [f for f in os.listdir(SOURCE_FOLDER) 
              if os.path.isfile(os.path.join(SOURCE_FOLDER, f)) and f.lower().endswith('.wav')]

if not wav_files:
    print("Error: No WAV files found")
    exit(1)

# Sort by number
def sort_key(name):
    import re
    nums = re.findall(r'\d+', name)
    return int(nums[0]) if nums else 999

wav_files.sort(key=sort_key)
total = len(wav_files)

print(f"Found {total} WAV files in: {SOURCE_FOLDER}\n")

# Create upload directory
os.makedirs(UPLOAD_DIR, exist_ok=True)

# Copy with simple names
print("Copying files with simple names...")
for i, filename in enumerate(wav_files, 1):
    src = os.path.join(SOURCE_FOLDER, filename)
    dest = os.path.join(UPLOAD_DIR, f"{i}.wav")
    shutil.copy2(src, dest)
    print(f"  [{i}/{total}] {i}.wav")

copied = len([f for f in os.listdir(UPLOAD_DIR) if f.endswith('.wav')])
if copied != total:
    print(f"\nError: Only {copied} files copied, expected {total}")
    exit(1)

print(f"\n✓ Files ready in: {UPLOAD_DIR}")
print("\nNext steps:")
print("1. Create thumbnail: Extract first page from PDF as JPG")
print("   Save as: f:\\ss\\public\\thumbnails\\chanakya_vyavaharniti.jpg")
print("\n2. Create GitHub release:")
print(f"   cd {UPLOAD_DIR}")
print(f"   gh release create {TAG_NAME} *.wav --repo {REPO} --title 'Chanakya Vyavaharniti'")
print("\n   OR via browser:")
print(f"   - Go to: https://github.com/{REPO}/releases/new")
print(f"   - Tag: {TAG_NAME}")
print(f"   - Upload all {total} WAV files from: {UPLOAD_DIR}")
print("\n3. Push to GitHub:")
print("   git add public/audio_list.json public/thumbnails/chanakya_vyavaharniti.jpg")
print("   git commit -m 'Add Chanakya Vyavaharniti audio book'")
print("   git push")
