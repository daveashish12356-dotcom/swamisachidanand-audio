# સ્કેન કરેલી PDF ને ટેક્સ્ટ PDF માં કન્વર્ટ કરવું

બધી બુક્સ સ્કેન/ફોટો પરથી બનેલી PDF છે, તેથી એપમાં ભારે રેન્ડર અને ક્રેશ થઈ શકે. પહેલા બધી PDF પર OCR (ગુજરાતી) ચલાવીને ટેક્સ્ટ લેયર ઉમેરો, પછી એ જ ફાઇલો એપના assets માં મૂકો.

## પગલાં

### 1. PC પર ઇન્સ્ટોલ કરો

- **Python 3** – [python.org](https://www.python.org/downloads/)  
- **ocrmypdf** – CMD/PowerShell માં:
  ```bash
  pip install ocrmypdf
  ```
- **Tesseract** – [GitHub tessdata](https://github.com/tesseract-ocr/tessdata) માંથી `guj.traineddata` ડાઉનલોડ કરીને Tesseract ની `tessdata` ફોલ્ડરમાં મૂકો.  
  (Windows: Tesseract ઇન્સ્ટોલ કર્યા પછી `C:\Program Files\Tesseract-OCR\tessdata` માં guj.traineddata મૂકો.)

### 2. કન્વર્ટ ચલાવો

પ્રોજેક્ટ ફોલ્ડર (`f:\ss`) માં:

```bash
python convert_pdfs_to_text.py
```

અથવા ડબલ‑ક્લિક: **run_convert_pdfs.bat**

બધી PDF `converted_pdfs` ફોલ્ડરમાં ટેક્સ્ટ લેયર સાથે બનશે. દરેક બુક માટે થોડો સમય લાગશે.

### 3. એપમાં ફાઇલો મૂકો

1. `app\src\main\assets` ની બેકઅપ લો (જૂની PDF ની કોપી રાખો).
2. `converted_pdfs` માંથી બધી `.pdf` ફાઇલ્સ કૉપી કરીને `app\src\main\assets` માં પેસ્ટ કરો (જૂની PDF ઓવરરાઇટ કરો).
3. એપ ફરી બિલ્ડ કરો અને ઇન્સ્ટોલ કરો.

આ પછી PDF માં ટેક્સ્ટ લેયર હશે, જેથી ભારે ઇમેજ-ઓનલી રેન્ડર ઓછું થઈ ક્રેશ ઘટવાની શક્યતા છે.
