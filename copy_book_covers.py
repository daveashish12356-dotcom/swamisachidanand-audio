# -*- coding: utf-8 -*-
"""Copy photos from BOOK_S COVER to public/book_covers as 1.jpg, 2.jpg, etc."""
import os
import shutil
import re

SRC = r"C:\Users\davea\Desktop\photos\BOOK_S COVER"
DEST = os.path.join(os.path.dirname(__file__), "public", "book_covers")

def sort_key(name):
    base = os.path.splitext(name)[0]
    nums = [int(n) for n in re.findall(r'\d+', base)]
    return (tuple(nums), base) if nums else ((9999,), base)

def main():
    os.makedirs(DEST, exist_ok=True)
    if not os.path.isdir(SRC):
        print("Source folder not found:", SRC)
        return
    files = [f for f in os.listdir(SRC) if f.lower().endswith(('.jpg', '.jpeg', '.png', '.webp'))]
    files.sort(key=sort_key)
    for i, f in enumerate(files, 1):
        ext = '.jpg'
        if f.lower().endswith('.png'): ext = '.png'
        elif f.lower().endswith('.webp'): ext = '.webp'
        dest_name = str(i) + ext
        src_path = os.path.join(SRC, f)
        dest_path = os.path.join(DEST, dest_name)
        try:
            shutil.copy2(src_path, dest_path)
            print("Copied:", i, f[:40])
        except Exception as e:
            print("Error:", f, e)
    print("Done. Total:", len(files))

if __name__ == "__main__":
    main()
