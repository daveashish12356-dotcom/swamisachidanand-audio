# રામાયણનું ચિંતન – પહેલાની બુક જેવી સેટઅપ

## 1. ડેસ્કટોપ ફોલ્ડર
- WAV ફાઇલો આ ફોલ્ડરમાં હોવી જોઈએ: `C:\Users\davea\Desktop\રામાયણ'નું ચિતન`
- ફાઇલો નંબરથી શરૂ થતી (1.xxx.wav, 2.xxx.wav, …) અથવા (1) (2) જેવા પાર્ટ્સ સાથે.

## 2. WAV → MP3 અને GitHub release
```powershell
cd f:\ss\audio-repo
.\CREATE_RAMAYAN_CHINTAN_RELEASE.ps1
```
- ffmpeg અને gh CLI જરૂરી.
- આ સ્ક્રિપ્ટ: WAV ને MP3 માં કન્વર્ટ કરે, `ramayan_chintan_mp3/` માં 1.mp3, 2.mp3, … બનાવે, `ramayan_chintan_parts.json` બનાવે, અને `swamisachidanand-audio` repo પર tag `ramayan_chintan` સાથે release બનાવે.

## 3. Audio list માં બુક ઉમેરો
Release બન્યા પછી:
```powershell
cd f:\ss
python audio-repo/add_ramayan_chintan_to_audio_list.py
```
આ `public/audio_list.json`, `audio_list.json` અને `app/src/main/assets/audio_list_fallback.json` માં રામાયણનું ચિંતન ઉમેરશે/અપડેટ કરશે.

## 4. સર્વર પર PDF અને થંબનેલ (gh-pages)
- `રામાયણનું ચિંતન.pdf` અને થંબનેલ `public/books/` અને `public/thumbnails/` માં મૂકો (અથવા સ્ક્રિપ્ટ ચલાવો):
```powershell
cd f:\ss
python add_ramayan_chintan_server.py
```
- સ્ક્રિપ્ટ ASCII નામ પણ બનાવશે: `ramayan_chintan.pdf`, `ramayan_chintan.jpg` (app આ URL થી લોડ કરશે).
- પછી `swamisachidanand-audio` repo ની gh-pages બ્રાન્ચ પર `public/` ની contents પુશ કરો (books/ અને thumbnails/ ફોલ્ડર્સ સહિત).

## 5. એપ રિલીઝ
- App માં already ઉમેરાઈ ગયું:
  - **ServerBookLoader**: `રામાયણનું ચિંતન.pdf` → `ramayan_chintan.pdf` / `ramayan_chintan.jpg` URLs.
  - **BookAdapter**: રામાયણ થંબનેલ માટે કેશ સ્કિપ.
  - **books_server_list.json**: પહેલેથી "રામાયણનું ચિંતન.pdf" છે.
- Build અને push:
```powershell
cd f:\ss
.\gradlew bundleRelease
# AAB: app\build\outputs\bundle\release\app-release.aab
# પછી Play Console પર upload કરો.
```

## સારાંશ
| Step | કામ |
|------|-----|
| 1 | ડેસ્કટોપ પર WAV ફોલ્ડર તૈયાર |
| 2 | `CREATE_RAMAYAN_CHINTAN_RELEASE.ps1` → MP3 + release |
| 3 | `add_ramayan_chintan_to_audio_list.py` → audio list + fallback |
| 4 | `add_ramayan_chintan_server.py` + gh-pages પર public/ પુશ |
| 5 | `gradlew bundleRelease` → AAB upload |
