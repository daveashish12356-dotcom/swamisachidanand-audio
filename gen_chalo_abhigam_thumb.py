#!/usr/bin/env python3
"""Generate chalo_abhigam_badlie.jpg thumbnail from PDF. Server only - no app change."""
import os
import shutil

try:
    import fitz  # PyMuPDF
except ImportError:
    print("pip install pymupdf")
    raise SystemExit(1)

# Try both spellings
PDF = r"C:\Users\davea\Desktop\ચાલો, અભિગમ બદલીએ.pdf"
PDF_ALT = r"C:\Users\davea\Desktop\ચાલો, આંભિગમ બદલીએ.pdf"
BOOK_ID = "chalo_abhigam_badlie"
W = 400

root = os.path.dirname(os.path.abspath(__file__))
out_name = BOOK_ID + ".jpg"
thumb_public = os.path.join(root, "public", "thumbnails", out_name)
thumb_audio = os.path.join(root, "audio-repo", "public", "thumbnails", out_name)
thumb_root = os.path.join(root, "audio-repo", "thumbnails", out_name)

def main():
    pdf_path = PDF
    if not os.path.isfile(pdf_path) and os.path.isfile(PDF_ALT):
        pdf_path = PDF_ALT
    if not os.path.isfile(pdf_path):
        print("PDF not found. Tried:", PDF, PDF_ALT)
        return
    doc = fitz.open(pdf_path)
    if len(doc) == 0:
        print("PDF has no pages")
        return
    page = doc[0]
    scale = W / float(page.rect.width or 1.0)
    mat = fitz.Matrix(scale, scale)
    pix = page.get_pixmap(matrix=mat, alpha=False)
    os.makedirs(os.path.dirname(thumb_public), exist_ok=True)
    pix.save(thumb_public, "jpeg")
    doc.close()
    print("Generated:", thumb_public)
    for d in [thumb_audio, thumb_root]:
        os.makedirs(os.path.dirname(d), exist_ok=True)
        shutil.copy2(thumb_public, d)
        print("Copied to:", d)

if __name__ == "__main__":
    main()
