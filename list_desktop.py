import os
import sys
d = os.path.expanduser("~/Desktop")
out = []
for x in sorted(os.listdir(d)):
    p = os.path.join(d, x)
    if os.path.isdir(p):
        wav = sum(1 for f in os.listdir(p) if f.lower().endswith('.wav'))
        mp3 = sum(1 for f in os.listdir(p) if f.lower().endswith('.mp3'))
        if wav or mp3:
            out.append(f"{x}\t{wav}\t{mp3}")
with open("f:/ss/desktop_audio_folders.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(out) if out else "No audio folders found")
