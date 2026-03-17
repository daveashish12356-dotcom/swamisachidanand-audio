# -*- coding: utf-8 -*-
"""Update mahabharat_jeevankathao in audio_list_fallback.json with real part titles."""
import json
import os

ASSETS = os.path.join(os.path.dirname(__file__), 'app', 'src', 'main', 'assets', 'audio_list_fallback.json')

# 61 parts: display title (order from original folder)
TITLES = [
    "1.મહાભારતની જીવનકથાઓ", "2.અર્પણ", "2.ભૂમિકા 1", "2.ભૂમિકા 2", "4.પાંડુરાજા",
    "5.ઉત્તંકકથા", "6.ઉત્તકકથા-2", "7.પુલોમા", "8.જરહ્કારુ", "9.નાગકન્યાનો ત્યાગ",
    "10.આસ્તિક", "11.શકુંતલા", "12.દુષ્યંતકથા", "13.કંચની કથા", "14.યયાતિ",
    "15. મત્સ્યગંધા", "16.હિડિમ્બા", "17. બકાસુરવધ", "18.તપતીકથા", "19.ઔર્વની કથા",
    "20.બ્રાહ્મણીનો શાપ", "21.તિલોત્તમા", "22.વર્ચાની કથા", "23.અર્જુંન-સુભદ્રાની કથા",
    "24.જરિતાની કથા", "25.કિર્મીરવધ", "26.ઉર્વશીનો અર્જુનને શાપ", "27.અગસ્ત્ય ત્રઠષિની કથા",
    "28.ગ્રષ્યશુંગની કથા", "29.યવકીત કથા", "30.સુશોભનાકથા", "32.પતિવ્રતા કથા",
    "33.ધર્મવ્યાધની કથા", "34.સ્કન્દજન્મ", "35.દુર્વાસાની તૃપ્તિ", "36. દ્રૌપદીહરણ 1",
    "36. દ્રૌપદીહરણ 2", "37.સત્યવાન-સાવિત્રી 1", "37.સત્યવાન-સાવિત્રી 2",
    "38.કીચકવધ 1", "38.કીચકવધ 2", "39.ઉત્તરાવિવાહ", "40.નહુષવધ",
    "41.શ્રીકૃષ્ણની મંત્રણા", "42.માધવી કન્યાની કથા 1", "42.માધવી કન્યાની કથા 2",
    "43.વિદુલાની કથા", "44.અમ્બાડકથા", "45.સોનાનો રાજકુમાર", "46.વૃદ્ધ કન્યા",
    "47.અશ્વત્થામાનો બદલો", "48. મૃત્યુકથા", "49.યુધિષ્ઠિરની સંન્યાસ ઇચ્છા",
    "50.શંખ અને લિખિત", "51.અન્તર્વામી કથા", "52.કૃતઘ્ન ગૌતમ", "53.તુલાધાર વૈશ્ય",
    "54.સુલભાની કથા 1", "54.સુલભાની કથા 2", "55.ગૌતમી", "56.પુરુષ-સ્રીઃ રતિસુખ",
    "57.અષ્ટાવકરની કથા 1", "57.અષ્ટાવકરની કથા 2", "58.વિપુલની કથા",
    "59.ઉતથ્યત્રકષિની કથા", "60.બભ્રુવાહનની કથા", "61.સાચો વજ્ઞ",
]

BASE = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/mahabharat_jeevankathao/"

def main():
    with open(ASSETS, 'r', encoding='utf-8') as f:
        data = json.load(f)
    for book in data.get('books', []):
        if book.get('id') != 'mahabharat_jeevankathao':
            continue
        parts = []
        for i in range(1, 62):
            title = TITLES[i - 1] if i <= len(TITLES) else "ભાગ " + str(i)
            parts.append({
                "id": str(i),
                "title": title,
                "url": BASE + str(i) + ".wav"
            })
        book['parts'] = parts
        break
    with open(ASSETS, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, separators=(',', ':'))
    print("Updated mahabharat_jeevankathao with 61 part titles. URLs: 1.wav ... 61.wav")
    print("Create GitHub release tag 'mahabharat_jeevankathao' and upload 1.wav to 61.wav for audio to load.")

if __name__ == '__main__':
    main()
