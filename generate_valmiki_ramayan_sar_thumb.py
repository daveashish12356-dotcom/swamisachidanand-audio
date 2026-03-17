#!/usr/bin/env python3
"""Generate વાલ્મીકિ-રામાયણ-સાર.jpg from first page of a similar PDF (placeholder).
Uses mahabharat_chintan.pdf if Valmiki PDF not found."""
import os

try:
    import fitz  # PyMuPDF
except ImportError:
    print("pip install pymupdf")
    raise SystemExit(1)

ROOT = os.path.dirname(os.path.abspath(__file__))
BOOKS = os.path.join(ROOT, "public", "books")
THUMB_DIR = os.path.join(ROOT, "public", "thumbnails")
OUT = os.path.join(THUMB_DIR, "વાલ્મીકિ-રામાયણ-સાર.jpg")
THUMB_WIDTH = 400

def main():
    os.makedirs(THUMB_DIR, exist_ok=True)
    # Use ramayan_chintan.pdf (similar Ramayan book) for Valmiki Ramayan Sar thumbnail
    ramayan_pdf = os.path.join(BOOKS, "ramayan_chintan.pdf")
    src = ramayan_pdf if os.path.isfile(ramayan_pdf) else None
    if not src:
        pdfs = [f for f in os.listdir(BOOKS) if f.lower().endswith(".pdf")]
        src = os.path.join(BOOKS, pdfs[0]) if pdfs else None
    if not src or not os.path.isfile(src):
        print("No PDF in public/books. Place વાલ્મીકિ-રામાયણ-સાર.jpg manually in public/thumbnails/")
        raise SystemExit(1)
    doc = fitz.open(src)
    page = doc[0]
    mat = fitz.Matrix(THUMB_WIDTH / page.rect.width, THUMB_WIDTH / page.rect.width)
    pix = page.get_pixmap(matrix=mat, alpha=False)
    pix.save(OUT)
    doc.close()
    print("Created:", OUT)

if __name__ == "__main__":
    main()
