# Add મહાભારતનું ચિંતન to public/books and public/thumbnails for server push
import os
import shutil

base = os.path.join(os.path.dirname(__file__), "public")
thumb_src = os.path.join(base, "thumbnails", "મહાભારતની જીવનકથાઓ.jpg")
thumb_dest = os.path.join(base, "thumbnails", "મહાભારતનું ચિંતન.jpg")
pdf_src = r"F:\52\'મહાભારત'નું ચિંતન.pdf"
pdf_dest = os.path.join(base, "books", "મહાભારતનું ચિંતન.pdf")

# 1) Thumbnail: copy from existing mahabharat book
if os.path.isfile(thumb_src):
    shutil.copy2(thumb_src, thumb_dest)
    print("OK: Thumbnail copied ->", thumb_dest)
else:
    print("Skip: Thumbnail source not found:", thumb_src)

# 2) PDF: copy from F:\52 if exists
if os.path.isfile(pdf_src):
    shutil.copy2(pdf_src, pdf_dest)
    print("OK: PDF copied ->", pdf_dest)
else:
    # Try without quotes in filename (Windows might have saved as this)
    alt = os.path.join(r"F:\52", "મહાભારતનું ચિંતન.pdf")
    if os.path.isfile(alt):
        shutil.copy2(alt, pdf_dest)
        print("OK: PDF copied (alt) ->", pdf_dest)
    else:
        print("Skip: PDF not found at F:\\52. Copy manually to public/books/મહાભારતનું ચિંતન.pdf")
