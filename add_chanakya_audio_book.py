# -*- coding: utf-8 -*-
"""Add ચાણક્યની વ્યવહારનીતિ audio book to audio_list.json"""
import json
import os
import shutil
import sys
from datetime import datetime

# Paths
AUDIO_FOLDER = r"C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ"
THUMBNAIL_PDF = r"C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ.pdf"
AUDIO_LIST_JSON = os.path.join(os.path.dirname(__file__), "public", "audio_list.json")
PUBLIC_THUMBNAILS = os.path.join(os.path.dirname(__file__), "public", "thumbnails")

# Book details
BOOK_ID = "chanakya_vyavaharniti"
BOOK_TITLE = "ચાણક્યની વ્યવહારનીતિ"
RELEASE_TAG = "chanakya_vyavaharniti"  # GitHub release tag
BASE_URL = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/"

def find_audio_files(folder_path):
    """Find all audio files (WAV/MP3) in folder, sorted by name"""
    if not os.path.isdir(folder_path):
        print(f"Error: Folder not found: {folder_path}")
        return []
    
    audio_files = []
    for f in os.listdir(folder_path):
        full_path = os.path.join(folder_path, f)
        if os.path.isfile(full_path):
            ext = os.path.splitext(f)[1].lower()
            if ext in ['.wav', '.mp3', '.m4a']:
                audio_files.append(f)
    
    # Sort files naturally (1.wav, 2.wav, ... 10.wav)
    def sort_key(name):
        base = os.path.splitext(name)[0]
        # Extract numbers from filename
        import re
        nums = re.findall(r'\d+', base)
        return int(nums[0]) if nums else 999
    
    audio_files.sort(key=sort_key)
    return audio_files

def copy_thumbnail(pdf_path, book_id):
    """Copy thumbnail from PDF or create JPG"""
    if not os.path.isfile(pdf_path):
        print(f"Warning: PDF not found: {pdf_path}")
        return None
    
    # Check if JPG version exists
    jpg_path = pdf_path.replace('.pdf', '.jpg')
    if os.path.isfile(jpg_path):
        dest_jpg = os.path.join(PUBLIC_THUMBNAILS, f"{book_id}.jpg")
        os.makedirs(PUBLIC_THUMBNAILS, exist_ok=True)
        shutil.copy2(jpg_path, dest_jpg)
        print(f"Copied thumbnail: {dest_jpg}")
        return f"{book_id}.jpg"
    
    # Try to extract first page from PDF as JPG
    dest_jpg = os.path.join(PUBLIC_THUMBNAILS, f"{book_id}.jpg")
    os.makedirs(PUBLIC_THUMBNAILS, exist_ok=True)
    
    try:
        from pdf2image import convert_from_path
        # Extract first page as image
        images = convert_from_path(pdf_path, first_page=1, last_page=1, dpi=150)
        if images:
            images[0].save(dest_jpg, 'JPEG', quality=85)
            print(f"Extracted thumbnail from PDF: {dest_jpg}")
            return f"{book_id}.jpg"
    except ImportError:
        print("pdf2image not installed. Install with: pip install pdf2image pillow")
        print("For now, copying PDF reference. You'll need to manually create JPG thumbnail.")
    except Exception as e:
        print(f"Could not extract thumbnail from PDF: {e}")
    
    # Fallback: copy PDF path info (user will need to create JPG manually)
    print(f"Note: Please create thumbnail manually:")
    print(f"  Source PDF: {pdf_path}")
    print(f"  Save as: {dest_jpg}")
    return None

def create_parts(audio_files, release_tag):
    """Create parts array from audio files"""
    parts = []
    for i, filename in enumerate(audio_files, 1):
        base_name = os.path.splitext(filename)[0]
        ext = os.path.splitext(filename)[1].lower()
        # Use simple numbering for GitHub release
        part_filename = f"{i}{ext}"
        url = f"{BASE_URL}{release_tag}/{part_filename}"
        
        parts.append({
            "id": str(i),
            "title": base_name,  # Will use filename as title, can be updated later
            "url": url
        })
    return parts

def add_book_to_json():
    """Add book entry to audio_list.json"""
    # Read existing JSON
    if not os.path.isfile(AUDIO_LIST_JSON):
        print(f"Error: audio_list.json not found: {AUDIO_LIST_JSON}")
        return False
    
    with open(AUDIO_LIST_JSON, 'r', encoding='utf-8') as f:
        content = f.read()
        if content.startswith('\ufeff'):
            content = content[1:]  # Remove BOM
        data = json.loads(content)
    
    # Check if book already exists
    books = data.get('books', [])
    for book in books:
        if book.get('id') == BOOK_ID:
            print(f"Book {BOOK_ID} already exists. Updating...")
            # Update existing book
            book['title'] = BOOK_TITLE
            book['parts'] = create_parts(find_audio_files(AUDIO_FOLDER), RELEASE_TAG)
            if thumbnail_name := copy_thumbnail(THUMBNAIL_PDF, BOOK_ID):
                book['thumbnailUrl'] = f"https://daveashish12356-dotcom.github.io/swamisachidanand-audio/thumbnails/{thumbnail_name}"
            break
    else:
        # Add new book
        new_book = {
            "id": BOOK_ID,
            "title": BOOK_TITLE,
            "parts": create_parts(find_audio_files(AUDIO_FOLDER), RELEASE_TAG)
        }
        if thumbnail_name := copy_thumbnail(THUMBNAIL_PDF, BOOK_ID):
            new_book['thumbnailUrl'] = f"https://daveashish12356-dotcom.github.io/swamisachidanand-audio/thumbnails/{thumbnail_name}"
        books.append(new_book)
        print(f"Added new book: {BOOK_ID}")
    
    # Update version and timestamp
    data['version'] = data.get('version', 1) + 1
    data['updated'] = datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")
    
    # Write back
    with open(AUDIO_LIST_JSON, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    print(f"\n✓ Updated {AUDIO_LIST_JSON}")
    return True

def main():
    import sys
    import io
    # Set UTF-8 encoding for stdout
    if sys.stdout.encoding != 'utf-8':
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    
    print("Adding Chanakya Vyavaharniti audio book...")
    print(f"Audio folder: {AUDIO_FOLDER}")
    print(f"Thumbnail PDF: {THUMBNAIL_PDF}\n")
    
    # Find audio files
    audio_files = find_audio_files(AUDIO_FOLDER)
    if not audio_files:
        print(f"\nError: No audio files found in {AUDIO_FOLDER}")
        print("Please check the folder path and ensure audio files (WAV/MP3) are present.")
        sys.exit(1)
    
    print(f"Found {len(audio_files)} audio files:")
    for i, f in enumerate(audio_files[:10], 1):
        print(f"  {i}. {f}")
    if len(audio_files) > 10:
        print(f"  ... and {len(audio_files) - 10} more")
    
    # Copy thumbnail
    print("\nProcessing thumbnail...")
    copy_thumbnail(THUMBNAIL_PDF, BOOK_ID)
    
    # Add to JSON
    print("\nUpdating audio_list.json...")
    if add_book_to_json():
        print("\n✓ Success!")
        print(f"\nNext steps:")
        print(f"1. Create GitHub release with tag: {RELEASE_TAG}")
        print(f"2. Upload audio files as: 1.wav, 2.wav, ... {len(audio_files)}.wav")
        print(f"3. Upload thumbnail to: public/thumbnails/{BOOK_ID}.jpg")
        print(f"4. Push changes to GitHub")
    else:
        print("\n✗ Failed to update audio_list.json")
        sys.exit(1)

if __name__ == '__main__':
    main()
