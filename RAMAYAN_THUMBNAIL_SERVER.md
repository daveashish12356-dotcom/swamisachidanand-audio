# રામાયણ થંબનેલ – સર્વરથી લોડ થાય તે માટે

એપમાં હવે થંબનેલ **એપમાં નથી**, **સર્વર URL** થી લોડ થાય છે.

## એપમાં કઈ URL વપરાય છે?

- **Books પેજ:** `ServerBookLoader` – રામાયણનું ચિંતન →  
  `https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/ramayan_chintan.jpg`

- **ઓડિયો પુસ્તકો:** `ServerAudioFragment.getServerThumbnailUrlForId("ramayan_chintan")` →  
  **એ જ URL:**  
  `https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/ramayan_chintan.jpg`

## થંબનેલ દેખાવા માટે શું કરવું?

આ URL પર ફાઇલ **live** હોવી જોઈએ. એટલે **swamisachidanand-audio** repo ની **gh-pages** બ્રાન્ચ પર `thumbnails/ramayan_chintan.jpg` અપલોડ/પુશ કરો.

### Option 1: GitHub પરથી (browser)

1. **swamisachidanand-audio** repo ખોલો:  
   https://github.com/daveashish12356-dotcom/swamisachidanand-audio  
2. બ્રાન્ચ **gh-pages** પસંદ કરો.  
3. `thumbnails` ફોલ્ડરમાં જાઓ (ન હોય તો Create new file થી `thumbnails/` બનાવો).  
4. **Upload files** થી આ ફાઇલ અપલોડ કરો:  
   `f:\ss\public\thumbnails\ramayan_chintan.jpg`  
   (નામ **ramayan_chintan.jpg** જ રાખો.)  
5. Commit / Push કરો.

### Option 2: Git / terminal

જો repo locally clone હોય (e.g. audio-repo અથવા swamisachidanand-audio):

```powershell
cd path\to\swamisachidanand-audio
git checkout gh-pages
mkdir -p thumbnails
copy "f:\ss\public\thumbnails\ramayan_chintan.jpg" thumbnails\ramayan_chintan.jpg
git add thumbnails/ramayan_chintan.jpg
git commit -m "Add ramayan_chintan thumbnail"
git push origin gh-pages
```

### ચેક કરવું

બ્રાઉઝરમાં આ link ખોલો:  
https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/ramayan_chintan.jpg  

- જો **ચિત્ર** દેખાય = સર્વર ઉપર ફાઇલ સાચી છે → એપમાં પણ Books અને ઓડિયો બુક્સમાં થંબનેલ સર્વરથી લોડ થશે.  
- જો **404** આવે = ફાઇલ gh-pages પર નથી → ઉપરના પગલાં પ્રમાણે અપલોડ/પુશ કરો.

## સારાંશ

| કામ | જગ્યા |
|-----|--------|
| થંબનેલ ફાઇલ (પ્રોજેક્ટમાં) | `f:\ss\public\thumbnails\ramayan_chintan.jpg` |
| અપલોડ કરવાની જગ્યા | repo **swamisachidanand-audio**, બ્રાન્ચ **gh-pages**, path **thumbnails/ramayan_chintan.jpg** |
| એપ જે URL વાપરે છે | `https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/ramayan_chintan.jpg` |

આ ફાઇલ gh-pages પર આવ્યા પછી એપમાં **કોઈ update વગર** Books અને ઓડિયો પુસ્તકો બંનેમાં રામાયણનું ચિંતનનું થંબનેલ **સર્વરથી જ** લોડ થશે.
