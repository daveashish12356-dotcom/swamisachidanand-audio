# -*- coding: utf-8 -*-
"""Create thumbnail and update audio_list.json with thumbnailUrl"""
import json
import os
import sys

PDF_PATH = r"C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ.pdf"
THUMBNAIL_DEST = r"f:\ss\public\thumbnails\chanakya_vyavaharniti.jpg"
AUDIO_LIST_JSON = r"f:\ss\public\audio_list.json"
BASE_URL = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/thumbnails/chanakya_vyavaharniti.jpg"

def create_thumbnail():
    """Extract thumbnail from PDF"""
    if not os.path.isfile(PDF_PATH):
        print(f"Error: PDF not found: {PDF_PATH}")
        return False
    
    os.makedirs(os.path.dirname(THUMBNAIL_DEST), exist_ok=True)
    
    try:
        from pdf2image import convert_from_path
        print("Extracting thumbnail from PDF...")
        images = convert_from_path(PDF_PATH, first_page=1, last_page=1, dpi=150)
        if images:
            images[0].save(THUMBNAIL_DEST, 'JPEG', quality=85)
            print(f"✓ Thumbnail created: {THUMBNAIL_DEST}")
            return True
    except ImportError:
        print("pdf2image not installed. Install with: pip install pdf2image pillow")
        print("Or manually create thumbnail from PDF first page")
        return False
    except Exception as e:
        print(f"Error creating thumbnail: {e}")
        return False

def update_json():
    """Add thumbnailUrl to chanakya_vyavaharniti book"""
    if not os.path.isfile(AUDIO_LIST_JSON):
        print(f"Error: audio_list.json not found")
        return False
    
    with open(AUDIO_LIST_JSON, 'r', encoding='utf-8') as f:
        content = f.read()
        if content.startswith('\ufeff'):
            content = content[1:]
        data = json.loads(content)
    
    # Find and update book
    updated = False
    for book in data.get('books', []):
        if book.get('id') == 'chanakya_vyavaharniti':
            book['thumbnailUrl'] = BASE_URL
            updated = True
            print(f"✓ Updated thumbnailUrl in JSON")
            break
    
    if not updated:
        print("Warning: chanakya_vyavaharniti book not found in JSON")
        return False
    
    # Write back
    with open(AUDIO_LIST_JSON, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    return True

if __name__ == '__main__':
    print("Creating thumbnail and updating JSON...\n")
    
    if create_thumbnail():
        if update_json():
            print("\n✓ Success! Thumbnail created and JSON updated.")
            print(f"\nThumbnail URL: {BASE_URL}")
            print("\nNext: Push to GitHub and create release")
        else:
            sys.exit(1)
    else:
        print("\nPlease create thumbnail manually or install pdf2image")
        sys.exit(1)
