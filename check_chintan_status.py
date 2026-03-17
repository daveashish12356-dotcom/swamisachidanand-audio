# -*- coding: utf-8 -*-
"""Check current status of mahabharat_chintan parts"""
import json

with open('app/src/main/assets/audio_list_fallback.json', encoding='utf-8') as f:
    data = json.load(f)

book = next(b for b in data['books'] if b['id'] == 'mahabharat_chintan')
parts = book['parts']

print("Parts around 59:")
print(f"  Part 57: {parts[56]['title']}")
print(f"  Part 58: {parts[57]['title']}")
print(f"  Part 59: {parts[58]['title']} (URL: {parts[58]['url'].split('/')[-1]})")
print(f"  Part 60: {parts[59]['title']}")

print("\n\nParts that still need titles:")
needs_title = [p for p in parts if p['title'].startswith('ભાગ ')]
for p in needs_title:
    file_num = p['url'].split('/')[-1].replace('.mp3', '')
    print(f"  Part {p['id']} (file {file_num}.mp3): {p['title']}")

print(f"\n\nTotal parts: {len(parts)}")
print(f"Parts needing titles: {len(needs_title)}")
