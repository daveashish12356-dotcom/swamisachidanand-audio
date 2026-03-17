# -*- coding: utf-8 -*-
"""Update mahabharat_chintan part titles from source JSON and fix missing parts"""
import json
import shutil
from pathlib import Path

audio_list_path = Path("public/audio_list.json")
fallback_path = Path("app/src/main/assets/audio_list_fallback.json")
source_json = Path("audio-repo/mahabharat_chintan_parts.json")

BASE = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/mahabharat_chintan/"

# Read source JSON (has original titles for parts 1-111)
with open(source_json, encoding='utf-8-sig') as f:
    source_parts = json.load(f)

# Create a map: file_number -> title
file_to_title = {}
for part in source_parts:
    url = part['url']
    file_num = url.split('/')[-1].replace('.mp3', '')
    file_to_title[file_num] = part['title']

print("Source titles by file number:")
for file_num in sorted(file_to_title.keys(), key=int):
    print(f"  File {file_num}.mp3: {file_to_title[file_num]}")

# Read current audio_list.json
with open(audio_list_path, encoding='utf-8-sig') as f:
    data = json.load(f)

# Find mahabharat_chintan book
for book in data['books']:
    if book['id'] == 'mahabharat_chintan':
        parts = book['parts']
        
        # Update all parts with titles from source
        updated_count = 0
        for part in parts:
            url = part['url']
            file_num = url.split('/')[-1].replace('.mp3', '')
            
            # If we have a title for this file number in source, use it
            if file_num in file_to_title:
                old_title = part['title']
                new_title = file_to_title[file_num]
                if old_title != new_title:
                    part['title'] = new_title
                    print(f"\nUpdated part {part['id']}:")
                    print(f"  Old: {old_title}")
                    print(f"  New: {new_title}")
                    updated_count += 1
        
        # For part 59: should use file 66.mp3
        # But we don't have title for 66.mp3 in source, so check what it should be
        part_59 = next((p for p in parts if p['id'] == '59'), None)
        if part_59:
            # Update URL to 66.mp3 if it's currently wrong
            if '59.mp3' in part_59['url']:
                part_59['url'] = BASE + "66.mp3"
                print(f"\nFixed part 59 URL to 66.mp3")
            
            # Title: Since we don't have it in source, keep generic for now
            # User will need to provide the actual title
            if part_59['title'] == "ભાગ 59" or part_59['title'] == "59.ઉશીનરની કથા":
                print(f"\nPart 59 title needs to be set manually (currently: {part_59['title']})")
        
        # For parts 112-134: they use files 112-134.mp3
        # We don't have titles in source, so they need manual update
        missing_titles = []
        for part in parts:
            if int(part['id']) >= 112:
                url = part['url']
                file_num = url.split('/')[-1].replace('.mp3', '')
                if part['title'].startswith('ભાગ '):
                    missing_titles.append(f"Part {part['id']} (file {file_num}.mp3): {part['title']}")
        
        if missing_titles:
            print(f"\n\nParts 112-134 need titles (currently generic):")
            for mt in missing_titles[:5]:  # Show first 5
                print(f"  {mt}")
            if len(missing_titles) > 5:
                print(f"  ... and {len(missing_titles) - 5} more")
        
        print(f"\n\nTotal updated: {updated_count} parts")
        break

# Write back
with open(audio_list_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, separators=(',', ':'))

print(f"\nUpdated {audio_list_path}")

# Copy to fallback
shutil.copy2(audio_list_path, fallback_path)
print(f"Copied to {fallback_path}")
