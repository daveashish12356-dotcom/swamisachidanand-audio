# GitHub Pages પર વાસ્તવિક PDF સર્વ કરવા (LFS pointer દૂર)

જો deploy પછી પણ app માં PDF ખોલતાં "સર્વર પર ફાઇલ ગલત છે" આવે, તો GitHub પર LFS ઑબ્જેક્ટ્સ પુશ થયા નથી. એક વાર નીચેનું ચલાવો:

## એક વાર (local PC પર)

1. **Git LFS install** (જો ન હોય):
   - Windows: `winget install GitHub.GitLFS` અથવા https://git-lfs.com
   - Mac: `brew install git-lfs` પછી `git lfs install`

2. **Real PDFs ખેંચો અને GitHub પર LFS push કરો:**

```bash
cd f:\ss
git lfs fetch --all
git lfs pull
git lfs push origin main
```

3. **GitHub Actions ફરી ચલાવો:**  
   Repo → **Actions** → **Deploy books and thumbnails to Pages** → **Run workflow**.

આ પછી deploy માં વાસ્તવિક PDFs `_site` માં કૉપી થશે અને Pages પર pointer નહીં, સાચી ફાઇલો serve થશે.
