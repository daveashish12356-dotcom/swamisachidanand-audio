# Copy મહાભારતનું ચિંતન PDF from F:\52 to app assets
import os
import shutil

f52 = r"F:\52"
dest = os.path.join(os.path.dirname(__file__), "app", "src", "main", "assets", "mahabharat_chintan.pdf")
for name in os.listdir(f52):
    if not name.endswith(".pdf"):
        continue
    if "ચિંતન" in name and ("મહાભારત" in name or "'" in name):
        src = os.path.join(f52, name)
        shutil.copy2(src, dest)
        print("Copied to", dest, "size", os.path.getsize(dest))
        break
else:
    print("PDF not found in F:\\52")
