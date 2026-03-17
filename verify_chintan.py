# -*- coding: utf-8 -*-
"""Verify mahabharat_chintan parts"""
import json

with open('app/src/main/assets/audio_list_fallback.json', encoding='utf-8') as f:
    data = json.load(f)

book = next(b for b in data['books'] if b['id'] == 'mahabharat_chintan')
print(f'Total parts: {len(book["parts"])}')
print(f'Part 59: {book["parts"][58]["title"]}')
print(f'Part 112: {book["parts"][111]["title"]}')
print(f'Part 134: {book["parts"][133]["title"]}')

# Check all IDs are sequential
ids = [int(p['id']) for p in book['parts']]
print(f'\nPart IDs range: {min(ids)} to {max(ids)}')
if sorted(ids) == list(range(1, 135)):
    print('✓ All 134 parts present!')
else:
    missing = set(range(1, 135)) - set(ids)
    print(f'✗ Missing IDs: {sorted(missing)}')
