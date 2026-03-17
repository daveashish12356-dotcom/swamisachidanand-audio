# Add રામાયણનું ચિંતન to public/books and public/thumbnails for server (gh-pages) push.
# Run from f:\ss. Copies PDF/thumbnail to public/ and creates ASCII names (ramayan_chintan.pdf / .jpg).

import os
import shutil
import sys

base = os.path.join(os.path.dirname(__file__), "public")
thumb_dest = os.path.join(base, "thumbnails", "રામાયણનું ચિંતન.jpg")
pdf_dest = os.path.join(base, "books", "રામાયણનું ચિંતન.pdf")

# 1) Thumbnail: copy from existing or use placeholder; you can replace with real cover later
thumb_src = os.path.join(base, "thumbnails", "મહાભારતનું ચિંતન.jpg")  # fallback if no Ramayan thumb
if os.path.isfile(thumb_src) and not os.path.isfile(thumb_dest):
    shutil.copy2(thumb_src, thumb_dest)
    print("Copied placeholder thumbnail:", thumb_dest)

# 2) PDF: try Desktop folder first (same folder as audio WAVs), then public/books, then F:\52
desktop_ramayan = os.path.join(os.path.expanduser("~"), "Desktop", "રામાયણ'નું ચિતન")
pdf_copied = os.path.isfile(pdf_dest)
for search_dir in [desktop_ramayan, os.path.join(os.path.dirname(__file__), "public", "books"), r"F:\52"]:
    if not os.path.isdir(search_dir):
        continue
    for name in os.listdir(search_dir):
        if not name.lower().endswith(".pdf"):
            continue
        if "રામાયણ" in name or "ramayan" in name.lower() or ("ચિતન" in name and "રામ" in name):
            src = os.path.join(search_dir, name)
            if os.path.isfile(src) and os.path.normpath(src) != os.path.normpath(pdf_dest):
                shutil.copy2(src, pdf_dest)
                pdf_copied = True
                print("Copied PDF:", src, "->", pdf_dest)
                break
            elif os.path.isfile(src):
                pdf_copied = True
                break
    if pdf_copied:
        break
if not pdf_copied:
    print("PDF not found. Put રામાયણનું ચિંતન.pdf in public/books/ or F:\\52 and run again.")

# 3) ASCII copies for app URL
guj_pdf = os.path.join(base, "books", "રામાયણનું ચિંતન.pdf")
guj_jpg = os.path.join(base, "thumbnails", "રામાયણનું ચિંતન.jpg")
ascii_pdf = os.path.join(base, "books", "ramayan_chintan.pdf")
ascii_jpg = os.path.join(base, "thumbnails", "ramayan_chintan.jpg")
if os.path.isfile(guj_pdf):
    shutil.copy2(guj_pdf, ascii_pdf)
    print("Created", ascii_pdf)
if os.path.isfile(guj_jpg):
    shutil.copy2(guj_jpg, ascii_jpg)
    print("Created", ascii_jpg)
