# -*- coding: utf-8 -*-
"""List all mahabharat_chintan parts to see pattern"""
import json

with open('app/src/main/assets/audio_list_fallback.json', encoding='utf-8') as f:
    data = json.load(f)

book = next(b for b in data['books'] if b['id'] == 'mahabharat_chintan')
parts = book['parts']

print("All parts with their titles and file numbers:")
for p in parts:
    # Extract file number from URL
    url = p['url']
    file_num = url.split('/')[-1].replace('.mp3', '')
    print(f"Part {p['id']:3s} -> File {file_num:3s}.mp3: {p['title']}")

print("\n\nParts around missing ones:")
print("Parts 55-65:")
for p in parts[54:65]:
    url = p['url']
    file_num = url.split('/')[-1].replace('.mp3', '')
    print(f"  Part {p['id']:3s} -> File {file_num:3s}.mp3: {p['title']}")

print("\nParts 108-115:")
for p in parts[107:115]:
    url = p['url']
    file_num = url.split('/')[-1].replace('.mp3', '')
    print(f"  Part {p['id']:3s} -> File {file_num:3s}.mp3: {p['title']}")
