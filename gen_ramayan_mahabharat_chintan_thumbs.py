#!/usr/bin/env python3
"""Generate ramayan_chintan.jpg and mahabharat_chintan.jpg for server thumbnails."""
import os
import shutil

try:
    import fitz  # PyMuPDF
except ImportError:
    print("Install PyMuPDF: pip install pymupdf")
    exit(1)

SCRIPT = os.path.dirname(os.path.abspath(__file__))
BOOKS = os.path.join(SCRIPT, "public", "books")
THUMB_PUBLIC = os.path.join(SCRIPT, "public", "thumbnails")
THUMB_AUDIO = os.path.join(SCRIPT, "audio-repo", "public", "thumbnails")
THUMB_ROOT = os.path.join(SCRIPT, "audio-repo", "thumbnails")
W = 400

def gen_from_pdf(pdf_path, out_name):
    if not os.path.exists(pdf_path):
        print("Not found:", pdf_path)
        return False
    doc = fitz.open(pdf_path)
    if len(doc) == 0:
        doc.close()
        return False
    page = doc[0]
    mat = fitz.Matrix(W / page.rect.width, W / page.rect.width)
    pix = page.get_pixmap(matrix=mat, alpha=False)
    out = os.path.join(THUMB_PUBLIC, out_name)
    os.makedirs(THUMB_PUBLIC, exist_ok=True)
    pix.save(out)
    doc.close()
    print("Generated:", out)
    return True

def copy_to_all(src, name):
    for d in [THUMB_AUDIO, THUMB_ROOT]:
        os.makedirs(d, exist_ok=True)
        dst = os.path.join(d, name)
        shutil.copy2(src, dst)
        print("Copied to:", dst)

# Mahabharat Chintan - from mahabharat_chintan.pdf
mahab_pdf = os.path.join(BOOKS, "mahabharat_chintan.pdf")
if gen_from_pdf(mahab_pdf, "mahabharat_chintan.jpg"):
    copy_to_all(os.path.join(THUMB_PUBLIC, "mahabharat_chintan.jpg"), "mahabharat_chintan.jpg")

# Ramayan Chintan - try PDF first (ramayan_chintan.pdf or Gujarati name)
ram_pdfs = [
    os.path.join(BOOKS, "ramayan_chintan.pdf"),
    os.path.join(BOOKS, "રામાયણનું ચિંતન.pdf"),
]
ram_src = None
for p in ram_pdfs:
    if os.path.exists(p):
        if gen_from_pdf(p, "ramayan_chintan.jpg"):
            ram_src = os.path.join(THUMB_PUBLIC, "ramayan_chintan.jpg")
            break
if ram_src:
    copy_to_all(ram_src, "ramayan_chintan.jpg")
else:
    # Try copy from existing Gujarati-named thumbnail
    guj_ram = os.path.join(THUMB_PUBLIC, "રામાયણનું ચિંતન.jpg")
    if os.path.exists(guj_ram):
        ram_out = os.path.join(THUMB_PUBLIC, "ramayan_chintan.jpg")
        shutil.copy2(guj_ram, ram_out)
        print("Copied Gujarati ramayan to ramayan_chintan.jpg")
        copy_to_all(ram_out, "ramayan_chintan.jpg")
    else:
        print("Ramayan PDF/thumb not found. Add ramayan_chintan.pdf to public/books/")
