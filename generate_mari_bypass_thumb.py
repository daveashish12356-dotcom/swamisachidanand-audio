#!/usr/bin/env python3
"""Generate mari_bypass_surgery.jpg from PDF. Run: python generate_mari_bypass_thumb.py"""
import os

try:
    import fitz  # PyMuPDF
except ImportError:
    print("pip install pymupdf")
    raise SystemExit(1)

ROOT = os.path.dirname(os.path.abspath(__file__))
THUMB_DIR = os.path.join(ROOT, "public", "thumbnails")
OUT = os.path.join(THUMB_DIR, "mari_bypass_surgery.jpg")
THUMB_WIDTH = 400

DESKTOP_PDF = os.path.join(os.path.expanduser("~"), "Desktop", "મારી બાયપાસ સર્જરી.pdf")
BOOKS_PDF = os.path.join(ROOT, "public", "books", "મારી બાયપાસ સર્જરી.pdf")

def main():
    os.makedirs(THUMB_DIR, exist_ok=True)
    src = DESKTOP_PDF if os.path.isfile(DESKTOP_PDF) else BOOKS_PDF
    if not src or not os.path.isfile(src):
        print("PDF nahi mili:", DESKTOP_PDF, "or", BOOKS_PDF)
        raise SystemExit(1)
    doc = fitz.open(src)
    page = doc[0]
    mat = fitz.Matrix(THUMB_WIDTH / page.rect.width, THUMB_WIDTH / page.rect.width)
    pix = page.get_pixmap(matrix=mat, alpha=False)
    pix.save(OUT)
    doc.close()
    print("Created: public/thumbnails/mari_bypass_surgery.jpg")

if __name__ == "__main__":
    main()
