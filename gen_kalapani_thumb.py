#!/usr/bin/env python3
"""Convert કલાપાણી..pdf (Desktop) first page to JPG thumbnail. Run from f:\ss."""
import os

try:
    import fitz  # PyMuPDF
except ImportError:
    print("pip install pymupdf")
    raise SystemExit(1)

ROOT = os.path.dirname(os.path.abspath(__file__))
THUMB_DIR = os.path.join(ROOT, "public", "thumbnails")
JPG_NAME = "કલાપાણી.jpg"
OUT = os.path.join(THUMB_DIR, JPG_NAME)
THUMB_WIDTH = 400

DESKTOP_PDF = os.path.join(os.path.expanduser("~"), "Desktop", "કલાપાણી..pdf")

def main():
    os.makedirs(THUMB_DIR, exist_ok=True)
    if not os.path.isfile(DESKTOP_PDF):
        print("PDF not found:", DESKTOP_PDF)
        return 1
    doc = fitz.open(DESKTOP_PDF)
    page = doc[0]
    mat = fitz.Matrix(THUMB_WIDTH / page.rect.width, THUMB_WIDTH / page.rect.width)
    pix = page.get_pixmap(matrix=mat, alpha=False)
    pix.save(OUT)
    doc.close()
    print("Created: public/thumbnails/Kalapani.jpg (thumbnail)")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
