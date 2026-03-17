# -*- coding: utf-8 -*-
import os
import shutil

books = os.path.join("public", "books")
backup = "chanakya_backup"
os.makedirs(backup, exist_ok=True)

best = None
for n in os.listdir(books):
    if not n.endswith(".pdf"):
        continue
    path = os.path.join(books, n)
    mb = os.path.getsize(path) / (1024 * 1024)
    if 50 < mb < 100:
        if best is None or mb > best[2]:
            best = (path, n, mb)
# List all > 50 MB for debugging
for n in os.listdir(books):
    if not n.endswith(".pdf"):
        continue
    path = os.path.join(books, n)
    mb = os.path.getsize(path) / (1024 * 1024)
    if mb > 50:
        with open(os.path.join(backup, "sizes.txt"), "a", encoding="utf-8") as f:
            f.write("%.1f MB\n" % mb)
if best:
    src, name, mb = best
    dst_pdf = os.path.join(backup, "chanakya.pdf")
    shutil.copy2(src, dst_pdf)
    with open(os.path.join(backup, "original_name.txt"), "w", encoding="utf-8") as f:
        f.write(name)
    print("OK", round(mb, 1), "MB")
else:
    print("NO_MATCH")
