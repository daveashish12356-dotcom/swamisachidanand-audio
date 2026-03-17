#!/usr/bin/env python3
"""
Generate thumbnail for ચાણક્યની વ્યવહારનીતિ book from PDF.
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
THUMB_WIDTH = 400

def main():
    pdf_path = PDF_PATH
    if not os.path.exists(pdf_path):
        print(f"PDF nahi mila: {pdf_path}")
        # Try alternative paths
        alt_paths = [
            os.path.join(os.path.expanduser("~"), "Desktop", "ચાણક્યની વ્યવહારનીતિ.pdf"),
            r"F:\52\ચાણક્યની વ્યવહારનીતિ.pdf"
        ]
        for alt in alt_paths:
            if os.path.exists(alt):
                pdf_path = alt
                print(f"Found PDF at: {pdf_path}")
                break
        else:
            print("PDF not found in any location")
            sys.exit(1)
    
    os.makedirs(THUMB_DIR, exist_ok=True)
    
    try:
        doc = fitz.open(pdf_path)
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
