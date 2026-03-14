# -*- coding: utf-8 -*-
"""
Fix books_store.json: assign correct img to each book based on scanned cover.
Mapping: photo N.jpg shows the book with this id (from reading cover title).
"""
import json
import os

BOOKS_JSON = os.path.join(os.path.dirname(__file__), "public", "books_store.json")

# Manual overrides: book id -> img filename (correct cover from Desktop, not from numbered photos)
MANUAL_IMG_OVERRIDE = {
    2: "shivtatvnirdesh.jpg",      # શિવતત્ત્વનિર્દેશ
    8: "vedantsamiksha.jpg",       # વેદાન્ત-સમીક્ષા
    10: "dharm.jpg",               # ધર્મ
    13: "adhogatimulvayvastha.jpg", # અધોગતિનું મૂળ વર્ણવ્યવસ્થા
    15: "nava_vichar.jpg",         # નવા વિચારો
    17: "prithvi_pradakshina.jpg", # પૃથ્વી-પ્રદક્ષિણા
    21: "himalay_na_hindole.jpg",  # હિમાલયના હીંડોળે
    26: "ijipt_isttrail_jankhi.jpg", # ઇજિપ્ત-ઇસ્ત્રાઇલની ઝાંખી
    27: "tanzaniya_17_divas.jpg",   # ટાન્ઝાનિયામાં 17 દિવસ
    29: "yurap_ni_atariye.jpg",     # યુરોપની અટારીએથી
    31: "rastr_na_salgata_prashno.jpg", # રાષ્ટ્રના સળગતા પ્રશ્નો
    32: "chin_mari_najare.jpg",     # ચીન-મારી નજરે
    34: "agvadh_ma_aradhna.jpg",    # અગવડોમાં આરાધના
    35: "pachim_thaine_rashiya.jpg", # પશ્ચિમ થઈને રશિયા
    39: "purv_ma_navu_pachim.jpg",  # પૂર્વમાં નવું પશ્ચિમ
    40: "africa_pravasna_sansmarano.jpg", # આફ્રિકા-પ્રવાસનાં સંસ્મરણો
    41: "apni_durbaltao.jpg",       # આપણી દુર્બળતાઓ
    42: "shrilanka_ni_safare.jpg",  # શ્રીલંકાની સફરે
    43: "dakshin_thaine_purv_ma.jpg", # દક્ષિણ-પૂર્વનો પ્રવાસ
    45: "rastiy_trith_adaman.jpg",  # રાષ્ટ્રીય તીર્થ આંદામાન
    46: "tarki_ane_ejippt.jpg",     # ટર્કી અને ઇજિપ્ત
    47: "staptiy_shourya_rajasthan.jpg", # સ્થાપત્ય અને શૌર્યની ભૂમિ રાજસ્થાન
    48: "phari_pacha_purv_ma.jpg",  # ફરી પાછા પૂર્વમાં
    49: "sikkim_ane_bhutan_no_pravas.jpg", # સિક્કિમ અને ભુતાનનો પ્રવાસ
    51: "purv_yurop_no_pravas.jpg", # પૂર્વ-યુરોપનો પ્રવાસ
    52: "moreshiyas_ane_dubai_no_pravas.jpg", # મોરેશિયસ અને દુબઇનો પ્રવાસ
    54: "mara_purvashram_na_samsmarano.jpg", # મારા પૂર્વાશ્રમનાં સંસ્મરણો
    55: "leh_ladakh_kargil_kashmir.jpg", # લેહ, લદાખ, કારગિલ, કાશ્મીર
    56: "himalay_na_char_dham.jpg", # હિમાલયના ચાર ધામ
    58: "kandhe_bandh_saar.jpg",   # કાન્હડદેપ્રબંધ-સાર
    59: "bodhgaya_netra_shradh.jpg", # બોધગયામાં નેત્રશ્રાદ્ધ
    61: "tamil_nadu_yatra.jpg",   # તામિલનાડુની યાત્રા
    63: "junagadh_no_azadi_jang.jpg", # જુનાગઢનો આઝાદીજંગ
    65: "ramayan_chintan.jpg",     # રામાયણનું ચિંતન
    68: "shahido_ni_krantigatha.jpg", # શહીદોની ક્રાંતિગાથાઓ
    71: "amarkantak_madhyapradesh_mahima.jpg", # અમરકંટક અને મધ્યપ્રદેશનો મહિમા
    73: "bhagvat_nu_chintan.jpg",  # ભાગવતનું ચિંતન
    74: "purv_ni_sat_baheno.jpg", # પૂર્વની સાત બહેનો
    77: "sacha_mahapurusho.jpg", # સાચા મહાપુરુષો
    78: "upnishad_kathao_ane_chintan.jpg", # ઉપનિષદોની કથાઓ અને ચિંતન
    80: "buddh_jatak_chintan_2.jpg", # બુદ્ધ-જાતક-ચિંતન:2
    81: "sardar_saheb_mari_najare.jpg", # સરદાર સાહેબ : મારી નજરે (PDF→JPG)
    82: "saurashtra_no_madhpudo.jpg", # સૌરાષ્ટ્રનો મધપૂડો
    85: "kranti_kathao.jpg",        # ક્રાંતિકથાઓ
    87: "vishnusahasranam_part1.jpg", # વિષ્ણુસહસ્ત્રનામ ભાગ-1
    89: "prashn_yej_uttar.jpg",     # પ્રશ્ન એ જ ઉત્તર
    91: "sardar_saheb_vadapradhan.jpg", # જો સરદાર સાહેબ વડાપ્રધાન બન્યા હોત તો ?
    93: "valmiki_ramayan_sar.jpg",  # વાલ્મીકી-રામાયણ-સાર
    94: "chanakya_ni_vyavhar_niti.jpg", # ચાણક્યની વ્યવહારનિતિ
    95: "munshi_premchand_punaravatar.jpg", # મુનશી પ્રેમચંદજીનો પુનરાવતાર
    96: "saurashtra_nu_shuratan.jpg", # સૌરાષ્ટ્રનું શૂરાતન
    98: "buddh_charit_chintan.jpg", # બુદ્ધ ચરિત્ર ચિંતન
    99: "shree_hanuman_chalisha.jpg", # શ્રી હનુમાન ચાલીસા
    100: "hampi_velur_halebidu_pravas.jpg", # હમ્પી, વેલુર અને હળેબીડુ પ્રવાસ
    101: "kabirji_nu_chintan.jpg", # શ્રી કબીરજીનું ચિંતન
    102: "lakshadweep_pravas.jpg", # લક્ષદ્વિપ-પ્રવાસ
    103: "kolambas_vasko_gama_bharat.jpg", # કોલંબસ અને વાસ્કો દ ગામા ભારતમાં કેમ ન પાક્યાં ?
    106: "koushani_rani_khet_pravas.jpg", # કૌસાની, રાણીખેત અને નૈનિતાલ ઊડતો પ્રવાસ
    107: "gondal_bapu_maharaj.jpg", # ગોંડલ-બાપુ મહારાજા ભગવતસિંહજી
    108: "tanot_mata_longowal_yuddh.jpg", # તનોટમાતા અને લોંગોવાલનું યુદ્ધ
    109: "mahadevi_ahilyabai_holkar.jpg", # મહાદેવી અહલ્યાબાઇ હોળકર
    110: "adi_sudharak_raja_rammohan_ray.jpg", # આદિ સુધારક રાજા રામમોહન રાય
    111: "kadva_mith_anubhavo.jpg", # કડવામીઠા અનુભવો
    112: "bijapur_thi_nanded.jpg", # બીજાપુરથી નાંદેડ
    115: "gova_pravas.jpg", # ગોવાપ્રવાસ
    113: "andaman_no_pravas.png", # આંદામાનનો પ્રવાસ
    117: "nepal_yatra.jpg", # નેપાળ-યાત્રા
    118: "khota_updesho_parinamo.jpg", # ખોટા ઉપદેશોનાં ખોટાં પરિણામ
    119: "haridwar_yatra.jpg", # હરિદ્વારની યાત્રા
    120: "ablamathi_prala_sindhutai_sapkal.jpg", # અબળામાંથી પ્રબળા સિંધુતાઇ સપકાળ
    121: "andaman_bijo_pravas.jpg", # આંદામાનનો બીજો પ્રવાસ
    122: "virata_parmo_dharm.jpg", # વીરતા પરમો ધર્મ
    123: "kashmir_no_tunko_etihas.jpg", # કાશ્મીરનો ટૂંકો ઇતિહાસ
    126: "muglo_antarik_hinsa.jpg", # મોગલોનો આંતરિક હિંસા કલહ
    128: "chedti_thi_barbadi.jpg", # છેડતીથી બરબાદી
    130: "patna_ane_panna.jpg", # પટના અને પન્ના
    131: "amara_vridhashramo_kadva_anubhav.jpg", # અમારા વૃદ્ધાશ્રમોના કડવા અનુભવો
    135: "ramanuj_pratima_darshan.jpg", # રામાનુજ પ્રતિમા દર્શન
    136: "patni_thi_thati_pida.jpg", # પત્નીઓથી થતી પીડા
    138: "kabutaro_akho_udhado.jpg", # કબૂતરો, આંખો ઉઘાડો !
    141: "mahan_ramanujachary.jpg", # મહાન રામાનુજાચાર્ય
    142: "prashn_yej_uttar_bhag1.jpg", # પ્રશ્ન એ જ ઉત્તર ભાગ-1
    143: "chintan_kanikavo.jpg", # ચિંતનકણિકાઓ
}

# Photo number (N in N.jpg) -> book id (from scanning cover; Gujarati title match)
# Built from reading each cover image and matching to books list.
PHOTO_TO_BOOK_ID = {
    1: 97,    # વસ્તુપાળ અને તેજપાળ
    2: 114,   # ગંગાસતીની અમર વાણી
    3: 9,     # મારા અનુભવો
    4: 86,    # કચ્છી કથાઓ
    7: 83,    # ફાંસીના વરરાજાઓ
    8: 144,   # ગુરુ નહિ, માર્ગદર્શક
    10: 12,   # ગીતા અને આપણા પ્રશ્નો
    14: 69,   # મહર્ષિ દયાનંદ સરસ્વતી
    15: 129,  # વીરાંગના મલાલા યુસુફજઇ
    17: 84,   # કાલાપાની
    18: 60,   # મહાભારતની જીવનકથાઓ
    20: 105,  # મહાન લિંકન
    21: 22,   # નવી દિશા
    22: 125,  # વિચારોનો ગુલદસ્તો
    23: 109,  # મહાદેવી અહલ્યાબાઈ હોળકર
    24: 23,   # આપણે અને પશ્ચિમ
    25: 90,   # મહાભારત-સાર
    26: 124,  # આપણી બોધકથાઓ
    27: 64,   # મહાભારતનું ચિંતન
    28: 36,   # યુદ્ધ અને યુદ્ધનેતા
    29: 30,   # દક્ષિણ આફ્રિકાની ઊડતી મુલાકાત
    30: 88,   # વિષ્ણુસહસ્ત્રનામ ભાગ-2
    31: 14,   # શું ઇશ્વર અવતાર લે છે ?
    32: 132,  # માનસ મંગલાચરણ મંથન
    34: 20,   # હવે તો જાગીએ
    35: 6,    # શ્રીકૃષ્ણલીલારહસ્ય
    37: 57,   # વાસ્તવિકતા
    38: 75,   # મહાન મહિલાઓ
    39: 92,   # વિદુર નિતિ
    40: 116,  # ઋષિવર શ્રી પ્રભાશંકર પટ્ટણી
    41: 76,   # ત્યાગ-અહિંસા-આતંકવાદ
    42: 25,   # પ્રશ્નોના મૂળમાં
    43: 18,   # ભારતીય યુદ્ધોનો સંક્ષિપ્ત ઇતિહાસ
    44: 24,   # ભારતમાં અંગ્રેજોનાં યુદ્ધો
    45: 3,    # પ્રવચનમંગલ
    46: 7,    # વિદેશયાત્રાના પ્રેરક પ્રસંગો
    47: 19,   # નવી આશા
    48: 38,   # મારા ઉપકારકો
    49: 44,   # માનવ-સંબંધો
    50: 72,   # સંતચરિત્રો અને ચિંતન
    51: 66,   # સિખ(શિખ)ધર્મના પક્ષમાં
    52: 110,  # આદિ સુધારક રાજા રામમોહન રાય
    53: 33,   # ઉપસંહાર
    55: 67,   # શિવાજીની શૌર્યગાથા
    56: 104,  # સિકંદર અને નેપોલિયન
    57: 133,  # કાશી-અયોધ્યા યાત્રા
    58: 137,  # પશ્વિમ બંગાળ મારી નજરે
    59: 4,    # આપણે અને સમાજ
    60: 62,   # પૌરાણિક કથાઓ
    61: 70,   # ગીતાજીનું ચિંતન (સંયુકત આવૃત્તિ)
    62: 5,    # સંસાર-રામાયણ
    63: 53,   # ચાણક્યની રાજનીતિ
    64: 1,    # ભારતીય દર્શનો
    65: 37,   # નર-નારીના સંબંધો...
    66: 50,   # ભર્તૃહરિનાં બે શતકો
    67: 11,   # ચાલો, અભિગમ બદલીએ
    68: 16,   # નવી દ્રષ્ટી
    69: 90,   # મહાભારત-સાર (alt cover)
    70: 139,  # અમારી નર્મદા - પરિક્રમા-સંયુક્ત
    71: 28,   # મારી બાયપાસ સર્જરી
    72: 140,  # સરદાર પટેલ વિરાટ પ્રતિભા
    73: 127,  # ઇઝરાયલનાં ચમત્કારિક પરાક્રમો
    74: 31,   # રાષ્ટ્રના સળગતા પ્રશ્નો
    78: 80,   # બુદ્ધ-જાતક-ચિંતન:2
    79: 79,   # બુદ્ધ-જાતક-ચિંતન:1
    80: 21,   # હિમાલયના હીંડોળે
    81: 63,   # જૂનાગઢનો આઝાદીજંગ
    82: 106,  # કૌસાની, રાણીખેત અને નૈનિતાલ ઊડતો પ્રવાસ
    83: 143,  # ચિંતનકણિકાઓ
    84: 65,   # રામાયણનું ચિંતન
    85: 93,   # વાલ્મીકી-રામાયણ-સાર
    86: 119,  # હરિદ્વારની યાત્રા
    87: 117,  # નેપાળ-યાત્રા
    88: 108,  # તનોટમાતા અને લોંગોવાલનું યુદ્ધ
    89: 54,   # મારા પૂર્વાશ્રમનાં સંસ્મરણો
    90: 111,  # કડવામીઠા અનુભવો
    91: 89,   # પ્રશ્ન એ જ ઉત્તર
    92: 138,  # કબૂતરો, આંખો ઉઘાડો !
    93: 99,   # શ્રી હનુમાન ચાલીસા
    94: 107,  # ગોંડલ-બાપુ મહારાજા ભગવતસિંહજી
    95: 120,  # અબળામાંથી પ્રબળા સિંધુતાઇ સપકાળ
    96: 101,  # શ્રી કબીરજીનું ચિંતન
    97: 94,   # ચાણક્યની વ્યવહારનિતિ
    98: 135,  # રામાનુજ પ્રતિમા દર્શન
    99: 134,  # માનસ મધપૂડો
    100: 131, # અમારા વૃદ્ધાશ્રમોના કડવા અનુભવો
    101: 130, # પટના અને પન્ના
    102: 126, # મોગલોનો આંતરિક હિંસા કલહ
    103: 123, # કાશ્મીરનો ટૂંકો ઇતિહાસ
    104: 121, # આંદામાનનો બીજો પ્રવાસ
    105: 118, # ખોટા ઉપદેશોનાં ખોટાં પરિણામ
    106: 112, # બીજાપુરથી નાંદેડ
    107: 103, # કોલંબસ અને વાસ્કો દ ગામા ભારતમાં કેમ ન પાક્યાં ?
    108: 102, # લક્ષદ્વિપ-પ્રવાસ
    109: 95,  # મુનશી પ્રેમચંદજીનો પુનરાવતાર
    110: 82,  # સૌરાષ્ટ્રનો મધપૂડો
    111: 74,  # પૂર્વની સાત બહેનો
    112: 73,  # ભાગવતનું ચિંતન
    113: 71,  # અમરકંટક અને મધ્યપ્રદેશનો મહિમા
    114: 77,  # સાચા મહાપુરુષો
    115: 58,  # કાન્હડદેપ્રબંધ-સાર
    116: 59,  # બોધગયામાં નેત્રશ્રાદ્ધ
    117: 55,  # લેહ, લદાખ, કારગિલ, કાશ્મીર
    118: 52,  # મોરેશિયસ અને દુબઇનો પ્રવાસ
    119: 141, # મહાન રામાનુજાચાર્ય
    120: 115, # ગોવાપ્રવાસ
    121: 100, # હમ્પી, વેલુર અને હળેબીડુ પ્રવાસ
    122: 96,  # સૌરાષ્ટ્રનું શૂરાતન
    123: 10,  # ધર્મ
    124: 13,  # અધોગતિનું મૂળ વર્ણવ્યવસ્થા
    125: 17,  # પૃથ્વી-પ્રદક્ષિણા
    126: 26,  # ઇજિપ્ત-ઇસ્ત્રાઇલની ઝાંખી
    127: 27,  # ટાન્ઝાનિયામાં 17 દિવસ
    128: 15,  # નવા વિચારો
    129: 29,  # યુરોપની અટારીએથી
    130: 32,  # ચીન-મારી નજરે
    131: 35,  # પશ્ચિમ થઈને રશિયા
    132: 39,  # પૂર્વમાં નવું પશ્ચિમ
    133: 40,  # આફ્રિકા-પ્રવાસનાં સંસ્મરણો
    134: 42,  # શ્રીલંકાની સફરે
    135: 43,  # દક્ષિણ-પૂર્વનો પ્રવાસ
    136: 45,  # રાષ્ટ્રીય તીર્થ આંદામાન
    137: 46,  # ટર્કી અને ઇજિપ્ત
    138: 48,  # ફરી પાછા પૂર્વમાં
    139: 98,  # બુદ્ધ ચરિત્ર ચિંતન
    140: 49,  # સિક્કિમ અને ભુતાનનો પ્રવાસ
    141: 91,  # જો સરદાર સાહેબ વડાપ્રધાન બન્યા હોત તો ?
    142: 51,  # પૂર્વ-યુરોપનો પ્રવાસ
    143: 85,  # ક્રાંતિકથાઓ
    144: 121, # આંદામાનનો બીજો પ્રવાસ (alt)
    145: 34,  # અગવડોમાં આરાધના
    146: 8,   # વેદાન્ત-સમીક્ષા
    148: 31,  # રાષ્ટ્રના સળગતા પ્રશ્નો (alt)
    149: 68,  # શહીદોની ક્રાંતિગાથાઓ
    150: 56,  # હિમાલયના ચાર ધામ
    151: 47,  # સ્થાપત્ય અને શૌર્યની ભૂમિ રાજસ્થાન
    152: 61,  # તામિલનાડુની યાત્રા
    153: 78,  # ઉપનિષદોની કથાઓ અને ચિંતન
    154: 2,   # શિવતત્ત્વનિર્દેશ
}


def main():
    with open(BOOKS_JSON, "r", encoding="utf-8") as f:
        data = json.load(f)

    # Build book_id -> img (first photo that maps to this book)
    book_to_img = {}
    for photo_num, book_id in PHOTO_TO_BOOK_ID.items():
        img_name = f"{photo_num}.jpg"
        if book_id not in book_to_img:
            book_to_img[book_id] = img_name
    # Apply manual overrides (correct covers from Desktop)
    for bid, img_name in MANUAL_IMG_OVERRIDE.items():
        book_to_img[bid] = img_name

    updated = 0
    for book in data["books"]:
        bid = int(book["id"])
        if bid in book_to_img:
            old = book["img"]
            book["img"] = book_to_img[bid]
            if old != book["img"]:
                updated += 1
                print(f"  id {bid}: {old} -> {book['img']}")

    with open(BOOKS_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"\nUpdated {updated} book img fields. Total mapped: {len(book_to_img)}.")
    print("Run again after adding more PHOTO_TO_BOOK_ID entries for remaining covers.")


if __name__ == "__main__":
    main()
