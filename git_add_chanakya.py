# -*- coding: utf-8 -*-
"""Add Chanakya PDF and thumbnail to git (Unicode paths)."""
import subprocess
import os

os.chdir(r"F:\ss")

# From books_server_list.json last entry
pdf_name = "ચાણક્યની રાજનીતિ.pdf"
thumb_name = "ચાણક્યની રાજનીતિ.jpg"

pdf_path = os.path.join("public", "books", pdf_name)
thumb_path = os.path.join("public", "thumbnails", thumb_name)

for p in (pdf_path, thumb_path):
    if os.path.exists(p):
        subprocess.run(["git", "add", p], check=True)
        print("Added")
    else:
        print("Not found")

# Also add list and script
subprocess.run(["git", "add", "public/books_server_list.json", "add_chanakya_rajniti_to_server.py"], check=True)
print("Added list and script.")
