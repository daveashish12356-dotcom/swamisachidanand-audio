# -*- coding: utf-8 -*-
"""Check mahabharat_chintan parts"""
import json

with open('app/src/main/assets/audio_list_fallback.json', encoding='utf-8') as f:
    data = json.load(f)

book = next(b for b in data['books'] if b['id'] == 'mahabharat_chintan')
print(f"Current parts in JSON: {len(book['parts'])}")
print(f"Last part ID: {book['parts'][-1]['id']}")
print(f"Last part title: {book['parts'][-1]['title']}")

# Get all part IDs
part_ids = [int(p['id']) for p in book['parts']]
print(f"\nPart IDs range: {min(part_ids)} to {max(part_ids)}")
print(f"Missing IDs:")
all_ids = set(range(1, 135))  # 1 to 134
missing = sorted(all_ids - set(part_ids))
print(missing[:20] if len(missing) > 20 else missing)
if len(missing) > 20:
    print(f"... and {len(missing) - 20} more")
