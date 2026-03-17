# -*- coding: utf-8 -*-
"""Copy Hanuman Chalisa MP3s to numbered 1.mp3..31.mp3 for GitHub release upload."""
import os
import shutil

# Same paths and sort as add_hanuman_chalisa_audio_book
AUDIO_FOLDER = r"C:\Users\davea\Desktop\શ્રી હનુમાન ચાલીસા"
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "audio-repo", "hanuman_release")

def find_audio_files(folder_path):
    import re
    if not os.path.isdir(folder_path):
        return []
    audio_files = []
    for f in os.listdir(folder_path):
        full = os.path.join(folder_path, f)
        if os.path.isfile(full):
            ext = os.path.splitext(f)[1].lower()
            if ext in ['.wav', '.mp3', '.m4a']:
                audio_files.append(f)
    def sort_key(name):
        base = os.path.splitext(name)[0]
        nums = [int(n) for n in re.findall(r'\d+', base)]
        return (0, tuple(nums), base) if nums else (1, (), base)
    audio_files.sort(key=sort_key)
    return audio_files

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    files = find_audio_files(AUDIO_FOLDER)
    if not files:
        print("No audio files in", AUDIO_FOLDER)
        return 1
    for i, name in enumerate(files, 1):
        src = os.path.join(AUDIO_FOLDER, name)
        dest = os.path.join(OUT_DIR, "%d.mp3" % i)
        if os.path.isfile(src):
            shutil.copy2(src, dest)
            print("%d -> %s" % (i, os.path.basename(dest)))
    print("Done. Release folder:", OUT_DIR)
    return 0

if __name__ == "__main__":
    exit(main())
