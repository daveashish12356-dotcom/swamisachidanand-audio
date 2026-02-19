// Merge ધર્મ book into audio_list.json
// Run: node merge_dharm.js

const fs = require('fs');
const path = require('path');

const scriptDir = __dirname;
const audioListPath = path.join(scriptDir, 'audio_list.json');
const publicAudioListPath = path.join(scriptDir, 'public', 'audio_list.json');
const partsJsonPath = path.join(scriptDir, 'dharm_parts.json');

// Read existing audio_list.json
let audioList = { version: 2, updated: new Date().toISOString(), books: [] };
if (fs.existsSync(audioListPath)) {
    const content = fs.readFileSync(audioListPath, 'utf8');
    try {
        audioList = JSON.parse(content);
    } catch (e) {
        console.error('Error parsing audio_list.json:', e.message);
        process.exit(1);
    }
}

// Read parts JSON
if (!fs.existsSync(partsJsonPath)) {
    console.error('Parts JSON nahi mila:', partsJsonPath);
    console.error('Pehle CREATE_DHARM_RELEASE.ps1 chalao.');
    process.exit(1);
}
let partsContent = fs.readFileSync(partsJsonPath, 'utf8');
// Remove BOM if present
if (partsContent.charCodeAt(0) === 0xFEFF) {
    partsContent = partsContent.slice(1);
}
const parts = JSON.parse(partsContent);

// Thumbnail URL (raw.githubusercontent.com from gh-pages branch - works immediately)
const thumbnailUrl = 'https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/dharm.jpg';

// New book entry
const newBook = {
    id: 'dharm',
    title: 'ધર્મ',
    thumbnailUrl: thumbnailUrl,
    parts: parts
};

// Remove existing dharm entry if present
audioList.books = audioList.books.filter(b => b.id !== 'dharm');

// Add new book
audioList.books.push(newBook);

// Update timestamp
audioList.updated = new Date().toISOString();

// Write to both locations
fs.writeFileSync(audioListPath, JSON.stringify(audioList, null, 2), 'utf8');
console.log('Updated:', audioListPath);

// Ensure public directory exists
const publicDir = path.dirname(publicAudioListPath);
if (!fs.existsSync(publicDir)) {
    fs.mkdirSync(publicDir, { recursive: true });
}
fs.writeFileSync(publicAudioListPath, JSON.stringify(audioList, null, 2), 'utf8');
console.log('Updated:', publicAudioListPath);

console.log(`\nDone! Book "${newBook.title}" added with ${parts.length} parts.`);
console.log('Next: Push to GitHub and thumbnail upload karo.');
