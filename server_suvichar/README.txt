સુવિચાર સર્વર સેટઅપ (Suvichar server setup)
=============================================

આ ફોલ્ડરમાંની ફાઇલો તમારા GitHub રિપો (swamisachidanand-audio) માં અપલોડ કરો, જેથી લિંક ઓનલાઇન થાય અને ON/OFF બટન કામ કરે.

1) લિંક ઓનલાઇન કરવી
---------------------
• suvichar_config.json ને રિપોના રૂટ પર અપલોડ કરો (જે ફોલ્ડરમાં index અથવા books છે તે જ રૂટ).
• લિંક આ રીતે ચાલશે:
  https://daveashish12356-dotcom.github.io/swamisachidanand-audio/suvichar_config.json

• સુવિચાર ઉમેરવા/બદલવા: suvichar_config.json ખોલો, "suvichar" array માં નવી entries ઉમેરો:
  { "text": "તમારો સુવિચાર.", "author": "સ્વામી શ્રીસચ્ચિદાનંદજી" }


2) ON / OFF બટન
----------------
• suvichar_control.html ને પણ રિપો રૂટ પર અપલોડ કરો.
• બ્રાઉઝરમાં ખોલો:
  https://daveashish12356-dotcom.github.io/swamisachidanand-audio/suvichar_control.html

• "સુવિચાર ON" – GitHub પર ફાઇલ ખુલશે; "suvicharEnabled" ને true કરો, Commit કરો.
• "સુવિચાર OFF" – "suvicharEnabled" ને false કરો, Commit કરો.

જો રિપોમાં default branch "gh-pages" હોય (ન કે main), તો suvichar_control.html માં બટનની લિંક બદલો:
  .../edit/gh-pages/suvichar_config.json
