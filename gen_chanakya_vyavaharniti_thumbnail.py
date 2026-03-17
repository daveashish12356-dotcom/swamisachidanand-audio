#!/usr/bin/env python3
"""
Generate thumbnail for ચાણક્યની વ્યવહારનીતિ from PDF.
Saves to public/thumbnails/chanakya_vyavaharniti.jpg
"""
import os
import sys

try:
    import fitz  # PyMuPDF
except ImportError:
    print("Install PyMuPDF: pip install pymupdf")
    sys.exit(1)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PDF_PATH = r"C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ.pdf"
THUMB_DIR = os.path.join(SCRIPT_DIR, "public", "thumbnails")
THUMB_PATH = os.path.join(THUMB_DIR, "chanakya_vyavaharniti.jpg")
AUDIO_REPO_THUMB = os.path.join(SCRIPT_DIR, "audio-repo", "public", "thumbnails", "chanakya_vyavaharniti.jpg")
THUMB_WIDTH = 400

def main():
    if not os.path.exists(PDF_PATH):
        print(f"PDF nahi mila: {PDF_PATH}")
        sys.exit(1)
    
    os.makedirs(THUMB_DIR, exist_ok=True)
    
    try:
        doc = fitz.open(PDF_PATH)
        if len(doc) == 0:
            print("PDF empty hai.")
            doc.close()
            sys.exit(1)
        
        page = doc[0]
        mat = fitz.Matrix(THUMB_WIDTH / page.rect.width, THUMB_WIDTH / page.rect.width)
        pix = page.get_pixmap(matrix=mat, alpha=False)
        pix.save(THUMB_PATH)
        doc.close()
        
        # Copy to audio-repo if it exists
        if os.path.isdir(os.path.dirname(AUDIO_REPO_THUMB)):
            import shutil
            os.makedirs(os.path.dirname(AUDIO_REPO_THUMB), exist_ok=True)
            shutil.copy2(THUMB_PATH, AUDIO_REPO_THUMB)
            print(f"Also saved to: {AUDIO_REPO_THUMB}")
        
        print(f"Thumbnail generated: {THUMB_PATH}")
        print(f"Size: {os.path.getsize(THUMB_PATH)} bytes")
        
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
