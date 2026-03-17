# -*- coding: utf-8 -*-
"""Get original file names for mahabharat_chintan parts 59 and 112-134"""
import json
from pathlib import Path

# Read current JSON to see what we have
with open('app/src/main/assets/audio_list_fallback.json', encoding='utf-8') as f:
    data = json.load(f)

book = next(b for b in data['books'] if b['id'] == 'mahabharat_chintan')
parts = book['parts']

print("Current titles for missing parts:")
print(f"Part 59: {parts[58]['title']}")
print(f"Part 112: {parts[111]['title']}")
print(f"Part 134: {parts[133]['title']}")

print("\nNeed to find original file names from source folder.")
print("Please check Desktop or F:\\52 for folder with 134 MP3 files.")
