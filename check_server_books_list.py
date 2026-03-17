# Compare public/books (server) vs books_server_list.json - sirf dekho, app me kuch nahi.
import os
import json

books_dir = "public/books"
list_path = "public/books_server_list.json"
out_path = "server_books_check.txt"

with open(list_path, "r", encoding="utf-8") as f:
    data = json.load(f)
in_list = set(data.get("fileNames", []))

if os.path.isdir(books_dir):
    on_disk = set(f for f in os.listdir(books_dir) if f.lower().endswith(".pdf"))
else:
    on_disk = set()

in_list_only = in_list - on_disk  # list me hai, server (disk) pe nahi
on_disk_only = on_disk - in_list  # server pe hai, list me nahi

lines = []
lines.append("Total in books_server_list.json: " + str(len(in_list)))
lines.append("Total PDFs in public/books: " + str(len(on_disk)))
lines.append("")
lines.append("--- List me hai par public/books me file NAHI (missing): " + str(len(in_list_only)))
for x in sorted(in_list_only):
    lines.append("  " + x)
lines.append("")
lines.append("--- Server (public/books) pe hai par LIST me NAHI (ye app me nahi dikhengi): " + str(len(on_disk_only)))
for x in sorted(on_disk_only):
    lines.append("  " + x)

with open(out_path, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print("Written", out_path)
