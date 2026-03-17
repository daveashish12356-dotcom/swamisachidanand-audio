#!/usr/bin/env python3
"""
Generate thumbnail for ધર્મ book from PDF.
Saves to public/thumbnails/dharm.jpg
"""
import os
import sys

try:
    import fitz  # PyMuPDF
except ImportError:
    print("Install PyMuPDF: pip install pymupdf")
    sys.exit(1)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PDF_PATH = r"C:\Users\davea\Desktop\ધર્મ.pdf"
THUMB_DIR = os.path.join(SCRIPT_DIR, "public", "thumbnails")
THUMB_PATH = os.path.join(THUMB_DIR, "dharm.jpg")
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
        
        print(f"Thumbnail generated: {THUMB_PATH}")
        print(f"Size: {os.path.getsize(THUMB_PATH)} bytes")
        
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
