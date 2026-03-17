# Thumbnail from original book PDF - કચ્છી કથાઓ
import os
from pathlib import Path

try:
    import fitz  # PyMuPDF
except ImportError:
    print("PyMuPDF (fitz) not installed. pip install pymupdf")
    raise SystemExit(1)

PDF_PATH = r"C:\Users\davea\Desktop\કચ્છી કથાઓ.pdf"
BOOK_ID = "kachchi_kathao"

def main():
    if not os.path.isfile(PDF_PATH):
        print("PDF not found:", PDF_PATH)
        return
    root = Path(__file__).resolve().parent
    thumb_name = BOOK_ID + ".jpg"
    out_public = root / "public" / "thumbnails" / thumb_name
    out_ghp = root.parent / "ss-ghp" / "thumbnails" / thumb_name
    out_public.parent.mkdir(parents=True, exist_ok=True)
    out_ghp.parent.mkdir(parents=True, exist_ok=True)

    doc = fitz.open(PDF_PATH)
    if len(doc) == 0:
        print("PDF has no pages")
        return
    page = doc[0]
    try:
        scale = 400.0 / float(page.rect.width or 1.0)
    except Exception:
        scale = 1.0
    mat = fitz.Matrix(scale, scale)
    pix = page.get_pixmap(matrix=mat, alpha=False)
    pix.save(str(out_public), "jpeg")
    doc.close()

    # Copy to ss-ghp for server
    import shutil
    shutil.copy2(str(out_public), str(out_ghp))
    print("Thumbnail:", out_public)
    print("Copied to ss-ghp:", out_ghp)

if __name__ == "__main__":
    main()
