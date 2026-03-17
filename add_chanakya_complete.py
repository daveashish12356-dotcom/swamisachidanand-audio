# -*- coding: utf-8 -*-
"""Complete setup for ચાણક્યની વ્યવહારનીતિ: WAV→MP3, PDF→JPG, JSON, Release, Push"""
import json
import os
import shutil
import subprocess
import sys
from datetime import datetime

# Set UTF-8 output
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Paths
AUDIO_FOLDER = r"C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ"
THUMBNAIL_PDF = r"C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ.pdf"
AUDIO_LIST_JSON = os.path.join(os.path.dirname(__file__), "public", "audio_list.json")
THUMBNAIL_DEST = os.path.join(os.path.dirname(__file__), "public", "thumbnails", "chanakya_vyavaharniti.jpg")
MP3_DIR = os.path.join(os.path.dirname(__file__), "chanakya_mp3")

# Book details
BOOK_ID = "chanakya_vyavaharniti"
BOOK_TITLE = "ચાણક્યની વ્યવહારનીતિ"
RELEASE_TAG = "chanakya_vyavaharniti"
REPO = "daveashish12356-dotcom/swamisachidanand-audio"
BASE_URL = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download"
THUMBNAIL_URL = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/thumbnails/chanakya_vyavaharniti.jpg"

def remove_old_entry():
    """Remove old chanakya entry from JSON if exists"""
    if not os.path.isfile(AUDIO_LIST_JSON):
        return
    
    with open(AUDIO_LIST_JSON, 'r', encoding='utf-8') as f:
        content = f.read()
        if content.startswith('\ufeff'):
            content = content[1:]
        data = json.loads(content)
    
    books = data.get('books', [])
    original_count = len(books)
    books = [b for b in books if b.get('id') != BOOK_ID]
    
    if len(books) < original_count:
        data['books'] = books
        with open(AUDIO_LIST_JSON, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"✓ Removed old {BOOK_ID} entry")

def find_audio_files():
    """Find and sort WAV files"""
    if not os.path.isdir(AUDIO_FOLDER):
        print(f"ERROR: Folder not found: {AUDIO_FOLDER}")
        sys.exit(1)
    
    wav_files = [f for f in os.listdir(AUDIO_FOLDER) 
                  if os.path.isfile(os.path.join(AUDIO_FOLDER, f)) and f.lower().endswith('.wav')]
    
    if not wav_files:
        print("ERROR: No WAV files found")
        sys.exit(1)
    
    # Sort by number
    import re
    def sort_key(name):
        nums = re.findall(r'\d+', name)
        return int(nums[0]) if nums else 999
    
    wav_files.sort(key=sort_key)
    return wav_files

def convert_wav_to_mp3(wav_files):
    """Convert WAV files to MP3"""
    print(f"\n{'='*60}")
    print("Converting WAV to MP3...")
    print(f"{'='*60}")
    
    # Check ffmpeg
    try:
        subprocess.run(['ffmpeg', '-version'], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("ERROR: ffmpeg not found. Install with: winget install ffmpeg")
        sys.exit(1)
    
    os.makedirs(MP3_DIR, exist_ok=True)
    
    # Clear old MP3s
    for f in os.listdir(MP3_DIR):
        if f.endswith('.mp3'):
            os.remove(os.path.join(MP3_DIR, f))
    
    total = len(wav_files)
    for i, filename in enumerate(wav_files, 1):
        src = os.path.join(AUDIO_FOLDER, filename)
        dest = os.path.join(MP3_DIR, f"{i}.mp3")
        
        print(f"[{i}/{total}] Converting {filename} -> {i}.mp3...", end=' ')
        
        # Convert using ffmpeg (ignore encoding errors in output)
        result = subprocess.run(
            ['ffmpeg', '-y', '-i', src, '-codec:a', 'libmp3lame', '-qscale:a', '2', dest],
            stderr=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL
        )
        
        if os.path.isfile(dest):
            print("✓")
        else:
            print("✗ FAILED")
            print(f"  Error: {result.stderr[:200]}")
    
    mp3_count = len([f for f in os.listdir(MP3_DIR) if f.endswith('.mp3')])
    if mp3_count != total:
        print(f"\nERROR: Only {mp3_count}/{total} MP3 files created")
        sys.exit(1)
    
    print(f"\n✓ Converted {mp3_count} files to MP3")
    return mp3_count

def create_thumbnail():
    """Extract thumbnail from PDF"""
    print(f"\n{'='*60}")
    print("Creating thumbnail from PDF...")
    print(f"{'='*60}")
    
    if not os.path.isfile(THUMBNAIL_PDF):
        print(f"ERROR: PDF not found: {THUMBNAIL_PDF}")
        sys.exit(1)
    
    os.makedirs(os.path.dirname(THUMBNAIL_DEST), exist_ok=True)
    
    try:
        from pdf2image import convert_from_path
        print("Extracting first page from PDF...")
        images = convert_from_path(THUMBNAIL_PDF, first_page=1, last_page=1, dpi=150)
        if images:
            images[0].save(THUMBNAIL_DEST, 'JPEG', quality=85)
            print(f"✓ Thumbnail created: {THUMBNAIL_DEST}")
            return True
    except ImportError:
        print("WARNING: pdf2image not installed.")
        print("Install with: pip install pdf2image pillow")
        print("Or manually extract first page from PDF and save as JPG")
        return False
    except Exception as e:
        print(f"ERROR creating thumbnail: {e}")
        return False

def add_to_json(wav_files):
    """Add book entry to audio_list.json"""
    print(f"\n{'='*60}")
    print("Updating audio_list.json...")
    print(f"{'='*60}")
    
    if not os.path.isfile(AUDIO_LIST_JSON):
        print(f"ERROR: audio_list.json not found")
        sys.exit(1)
    
    with open(AUDIO_LIST_JSON, 'r', encoding='utf-8') as f:
        content = f.read()
        if content.startswith('\ufeff'):
            content = content[1:]
        data = json.loads(content)
    
    # Create parts array
    parts = []
    for i, filename in enumerate(wav_files, 1):
        base_name = os.path.splitext(filename)[0]
        parts.append({
            "id": str(i),
            "title": base_name,
            "url": f"{BASE_URL}/{RELEASE_TAG}/{i}.mp3"
        })
    
    # Create book entry
    new_book = {
        "id": BOOK_ID,
        "title": BOOK_TITLE,
        "thumbnailUrl": THUMBNAIL_URL,
        "parts": parts
    }
    
    # Remove old entry if exists
    books = data.get('books', [])
    books = [b for b in books if b.get('id') != BOOK_ID]
    books.append(new_book)
    data['books'] = books
    
    # Update version and timestamp
    data['version'] = data.get('version', 1) + 1
    data['updated'] = datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")
    
    # Write back
    with open(AUDIO_LIST_JSON, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    print(f"✓ Added book with {len(parts)} parts")
    print(f"✓ Thumbnail URL: {THUMBNAIL_URL}")

def create_release():
    """Create GitHub release"""
    print(f"\n{'='*60}")
    print("Creating GitHub release...")
    print(f"{'='*60}")
    
    mp3_files = [f for f in os.listdir(MP3_DIR) if f.endswith('.mp3')]
    if not mp3_files:
        print("ERROR: No MP3 files found")
        sys.exit(1)
    
    mp3_files.sort(key=lambda x: int(x.replace('.mp3', '')))
    mp3_paths = [os.path.join(MP3_DIR, f) for f in mp3_files]
    
    # Check GitHub CLI
    try:
        subprocess.run(['gh', '--version'], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("WARNING: GitHub CLI (gh) not found.")
        print("Create release manually:")
        print(f"  https://github.com/{REPO}/releases/new")
        print(f"  Tag: {RELEASE_TAG}")
        print(f"  Upload {len(mp3_files)} MP3 files from: {MP3_DIR}")
        return False
    
    print(f"Creating release '{RELEASE_TAG}' with {len(mp3_files)} MP3 files...")
    
    cmd = ['gh', 'release', 'create', RELEASE_TAG] + mp3_paths + [
        '--repo', REPO,
        '--title', BOOK_TITLE
    ]
    
    result = subprocess.run(cmd, capture_output=True, text=True)
    
    if result.returncode == 0:
        print("✓ Release created successfully")
        return True
    else:
        print("✗ Release creation failed")
        print(f"  Error: {result.stderr}")
        print("\nCreate release manually:")
        print(f"  https://github.com/{REPO}/releases/new")
        return False

def push_to_git():
    """Push changes to GitHub"""
    print(f"\n{'='*60}")
    print("Pushing to GitHub...")
    print(f"{'='*60}")
    
    files_to_add = [AUDIO_LIST_JSON]
    if os.path.isfile(THUMBNAIL_DEST):
        files_to_add.append(THUMBNAIL_DEST)
    
    # Git add
    for f in files_to_add:
        rel_path = os.path.relpath(f, os.path.dirname(__file__))
        result = subprocess.run(['git', 'add', rel_path], cwd=os.path.dirname(__file__))
        if result.returncode == 0:
            print(f"✓ Added: {rel_path}")
    
    # Git commit
    result = subprocess.run(
        ['git', 'commit', '-m', f'Add {BOOK_TITLE} audio book'],
        cwd=os.path.dirname(__file__)
    )
    
    if result.returncode == 0:
        print("✓ Committed changes")
    else:
        print("⚠ No changes to commit (or already committed)")
    
    # Git push
    result = subprocess.run(['git', 'push'], cwd=os.path.dirname(__file__))
    
    if result.returncode == 0:
        print("✓ Pushed to GitHub")
        return True
    else:
        print("✗ Push failed. Push manually:")
        print("  git push")
        return False

def main():
    print("=" * 60)
    print("ચાણક્યની વ્યવહારનીતિ - Complete Setup")
    print("=" * 60)
    
    # Step 1: Remove old entry
    print("\n[1/6] Removing old entry...")
    remove_old_entry()
    
    # Step 2: Find audio files
    print("\n[2/6] Finding audio files...")
    wav_files = find_audio_files()
    print(f"✓ Found {len(wav_files)} WAV files")
    
    # Step 3: Convert to MP3
    print("\n[3/6] Converting WAV to MP3...")
    mp3_count = convert_wav_to_mp3(wav_files)
    
    # Step 4: Create thumbnail
    print("\n[4/6] Creating thumbnail...")
    thumbnail_ok = create_thumbnail()
    if not thumbnail_ok:
        print("⚠ Thumbnail not created. Please create manually:")
        print(f"  Source: {THUMBNAIL_PDF}")
        print(f"  Save as: {THUMBNAIL_DEST}")
    
    # Step 5: Update JSON
    print("\n[5/6] Updating audio_list.json...")
    add_to_json(wav_files)
    
    # Step 6: Create release
    print("\n[6/6] Creating GitHub release...")
    release_ok = create_release()
    
    # Step 7: Push to git
    print("\n[7/7] Pushing to GitHub...")
    push_ok = push_to_git()
    
    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"✓ MP3 files: {mp3_count} ready in {MP3_DIR}")
    print(f"{'✓' if thumbnail_ok else '⚠'} Thumbnail: {THUMBNAIL_DEST}")
    print(f"✓ JSON: Updated with {mp3_count} parts")
    print(f"{'✓' if release_ok else '⚠'} Release: {'Created' if release_ok else 'Create manually'}")
    print(f"{'✓' if push_ok else '⚠'} Git: {'Pushed' if push_ok else 'Push manually'}")
    
    if not release_ok:
        print(f"\n⚠ Create release manually:")
        print(f"  https://github.com/{REPO}/releases/new")
        print(f"  Tag: {RELEASE_TAG}")
        print(f"  Upload MP3 files from: {MP3_DIR}")
    
    print("\n✓ Setup complete! App will auto-scan and show the book.")

if __name__ == '__main__':
    main()
