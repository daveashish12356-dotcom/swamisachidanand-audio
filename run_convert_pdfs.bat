@echo off
REM Convert all scanned PDFs to text-layer PDFs (Gujarati OCR). Run on PC first.
cd /d "%~dp0"
echo Installing ocrmypdf if needed...
pip install ocrmypdf
echo.
echo Running conversion (this may take a long time - one PDF per book)...
python convert_pdfs_to_text.py
echo.
pause
