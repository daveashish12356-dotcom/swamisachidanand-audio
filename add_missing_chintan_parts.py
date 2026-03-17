# -*- coding: utf-8 -*-
"""Add missing parts 59 and 112-134 to mahabharat_chintan"""
import json
import shutil
from pathlib import Path

audio_list_path = Path("public/audio_list.json")
fallback_path = Path("app/src/main/assets/audio_list_fallback.json")

BASE = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/mahabharat_chintan/"

# Read audio_list.json
with open(audio_list_path, encoding='utf-8-sig') as f:
    data = json.load(f)

# Find mahabharat_chintan book
for book in data['books']:
    if book['id'] == 'mahabharat_chintan':
        # Get current parts
        current_parts = book['parts']
        current_ids = {int(p['id']) for p in current_parts}
        
        # Missing IDs: 59 and 112-134
        missing_ids = [59] + list(range(112, 135))
        
        print(f"Current parts: {len(current_parts)}")
        print(f"Missing IDs: {missing_ids}")
        
        # Create a map of existing parts by ID
        parts_map = {int(p['id']): p for p in current_parts}
        
        # Add missing parts
        for part_id in missing_ids:
            # Use generic title for now - can be updated later
            title = f"ભાગ {part_id}"
            parts_map[part_id] = {
                "id": str(part_id),
                "title": title,
                "url": BASE + str(part_id) + ".mp3"
            }
        
        # Rebuild parts list in order
        book['parts'] = [parts_map[i] for i in sorted(parts_map.keys())]
        
        print(f"Updated to {len(book['parts'])} parts")
        print(f"Part IDs range: {min(int(p['id']) for p in book['parts'])} to {max(int(p['id']) for p in book['parts'])}")
        break

# Write back
with open(audio_list_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, separators=(',', ':'))

print(f"\nUpdated {audio_list_path}")

# Copy to fallback
shutil.copy2(audio_list_path, fallback_path)
print(f"Copied to {fallback_path}")
