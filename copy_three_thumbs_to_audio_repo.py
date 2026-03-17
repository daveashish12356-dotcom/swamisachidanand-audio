#!/usr/bin/env python3
"""Copy kachchi_kathao.jpg, kandadeprabandh_sar.jpg, geetaji_chintan.jpg
   from public/thumbnails to audio-repo/public/thumbnails and audio-repo/thumbnails."""
import os
import shutil

SCRIPT = os.path.dirname(os.path.abspath(__file__))
SRC_DIR = os.path.join(SCRIPT, "public", "thumbnails")
AUDIO_PUBLIC = os.path.join(SCRIPT, "audio-repo", "public", "thumbnails")
AUDIO_ROOT = os.path.join(SCRIPT, "audio-repo", "thumbnails")

NAMES = ["kachchi_kathao.jpg", "kandadeprabandh_sar.jpg", "geetaji_chintan.jpg"]

def main():
    for name in NAMES:
        src = os.path.join(SRC_DIR, name)
        if not os.path.isfile(src):
            print("Missing:", src)
            continue
        for dst_dir in [AUDIO_PUBLIC, AUDIO_ROOT]:
            os.makedirs(dst_dir, exist_ok=True)
            dst = os.path.join(dst_dir, name)
            shutil.copy2(src, dst)
            print("Copied to:", dst)

if __name__ == "__main__":
    main()
