# -*- coding: utf-8 -*-
"""Swap URLs for સુલભાની કથા 1 and 2 in mahabharat_jeevankathao"""
import json
import shutil
from pathlib import Path

audio_list_path = Path("public/audio_list.json")
fallback_path = Path("app/src/main/assets/audio_list_fallback.json")

# Read audio_list.json
with open(audio_list_path, encoding='utf-8-sig') as f:
    data = json.load(f)

# Find mahabharat_jeevankathao book
for book in data['books']:
    if book['id'] == 'mahabharat_jeevankathao':
        part1 = None
        part2 = None
        
        # Find the two parts
        for part in book['parts']:
            if part['title'] == '54.સુલભાની કથા 1':
                part1 = part
            elif part['title'] == '54.સુલભાની કથા 2':
                part2 = part
        
        if part1 and part2:
            # Swap URLs
            url1 = part1['url']
            url2 = part2['url']
            part1['url'] = url2
            part2['url'] = url1
            print(f"Swapped URLs:")
            print(f"  54.સુલભાની કથા 1: {url2}")
            print(f"  54.સુલભાની કથા 2: {url1}")
        else:
            print("ERROR: Could not find both parts")
            if not part1:
                print("  Missing: 54.સુલભાની કથા 1")
            if not part2:
                print("  Missing: 54.સુલભાની કથા 2")
        break

# Write back
with open(audio_list_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, separators=(',', ':'))

print(f"\nUpdated {audio_list_path}")

# Copy to fallback
shutil.copy2(audio_list_path, fallback_path)
print(f"Copied to {fallback_path}")
