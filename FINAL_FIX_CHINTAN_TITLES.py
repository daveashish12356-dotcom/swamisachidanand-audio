# -*- coding: utf-8 -*-
"""
Final script to fix mahabharat_chintan part titles.
Update the TITLES dict below with correct titles, then run this script.
"""
import json
import shutil
from pathlib import Path

audio_list_path = Path("public/audio_list.json")
fallback_path = Path("app/src/main/assets/audio_list_fallback.json")

BASE = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/mahabharat_chintan/"

# TODO: Update these with correct titles from source files
TITLES_TO_FIX = {
    "59": "59.નર્મદાતટે પહોંચવું",  # FIXME: This is wrong, same as part 57. Need correct title for file 66.mp3
    "112": "112.???",  # FIXME: Need actual title
    "115": "115.???",  # FIXME: Need actual title  
    "117": "117.???",  # FIXME: Need actual title
    "125": "125.???",  # FIXME: Need actual title
    "129": "129.???",  # FIXME: Need actual title
    "134": "134.???"   # FIXME: Need actual title
}

# Read current audio_list.json
with open(audio_list_path, encoding='utf-8-sig') as f:
    data = json.load(f)

# Find mahabharat_chintan book
for book in data['books']:
    if book['id'] == 'mahabharat_chintan':
        parts = book['parts']
        
        updated = []
        for part in parts:
            part_id = part['id']
            if part_id in TITLES_TO_FIX:
                old_title = part['title']
                new_title = TITLES_TO_FIX[part_id]
                if new_title != "???" and new_title != old_title:
                    part['title'] = new_title
                    updated.append(f"Part {part_id}: {old_title} -> {new_title}")
        
        if updated:
            print("Updated titles:")
            for u in updated:
                print(f"  {u}")
        else:
            print("No updates made. Please update TITLES_TO_FIX dict with correct titles.")
        
        break

# Write back
with open(audio_list_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, separators=(',', ':'))

print(f"\nUpdated {audio_list_path}")

# Copy to fallback
shutil.copy2(audio_list_path, fallback_path)
print(f"Copied to {fallback_path}")
