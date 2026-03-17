# Copy મહાભારતનું ચિંતન.pdf/.jpg to mahabharat_chintan.pdf/.jpg for ASCII URL
import os
import shutil

base = os.path.join(os.path.dirname(__file__), "public")
guj_pdf = os.path.join(base, "books", "મહાભારતનું ચિંતન.pdf")
guj_jpg = os.path.join(base, "thumbnails", "મહાભારતનું ચિંતન.jpg")
ascii_pdf = os.path.join(base, "books", "mahabharat_chintan.pdf")
ascii_jpg = os.path.join(base, "thumbnails", "mahabharat_chintan.jpg")
if os.path.isfile(guj_pdf):
    shutil.copy2(guj_pdf, ascii_pdf)
    print("Created", ascii_pdf)
else:
    print("Source PDF not found:", guj_pdf)
if os.path.isfile(guj_jpg):
    shutil.copy2(guj_jpg, ascii_jpg)
    print("Created", ascii_jpg)
else:
    print("Source JPG not found:", guj_jpg)
