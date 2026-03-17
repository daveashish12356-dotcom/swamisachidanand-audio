# Verify: app book list vs server book list - sirf verify, kuch mat badlo.
import json

# Repo server list (what we have in public/books_server_list.json)
with open("public/books_server_list.json", "r", encoding="utf-8") as f:
    repo_data = json.load(f)
repo_list = set(repo_data.get("fileNames", []))

# Live server list (fetched from URL - paste or load from file if you have it)
# Live = what app gets from URL (fetched 2025-03-01)
live_data = json.loads("""
{"fileNames":["અમરકંટક અને મધ્યપ્રદેશનો મહિમા.pdf","આપણી દુર્બળતાઓ.pdf","આફ્રિકા-પ્રવાસનાં સંસ્મરણો.pdf","આવેગો અને લાગણીઓ.pdf","ઉપનિષદોની કથાઓ અને ચિંતન.pdf","ઉપસંહાર.pdf","કચ્છી કથાઓ.pdf","કાંડદેવબંધ-સાર.pdf","ક્રાંતિકથાઓ.pdf","ગીતાજ્ઞાન ચિંતન.pdf","ચાણક્યની વ્યવહારનીતિ.pdf","ચાલો, અભિગમ બદલીએ.pdf","ચિંતન–કિરણો.pdf","ટર્કી અને ઈજિપ્ત.pdf","ત્યાગ-અહિંસા-આત્મવાદ.pdf","દક્ષિણ આફ્રિકાની ઉલટી મુલાકાત.pdf","ધર્મ.pdf","નર-નારીના સંબંધો, લગ્નસંસ્થા તથા આચારો અને લાગણીઓ સ્વામી સચ્ચિદાનંદ.pdf","નવી દૃષ્ટિ.pdf","પૂર્વ યુરોપનો પ્રવાસ.pdf","પૂર્વની સાત બહેનો.pdf","પૂર્વમાં નવું પશ્ચિમ.pdf","પૌરાણિક કથાઓ.pdf","પ્રશ્ન એ જ ઉત્તર.pdf","ફરી પાછા પૂર્વમાં.pdf","બુદ્ધચરિત્રચિંતન.pdf","બુદ્ધ-જાતક-ચિંતન ૧.pdf","બોધગમામાં નેત્રશ્રદ્ધા.pdf","ભર્તૃહરિનાં બે શતકો.pdf","ભાગવતનું ચિંતન.pdf","ભારતીય દર્શનો.pdf","મહર્ષિ દયાનંદ સરસ્વતી.pdf","મહાન મહિલાઓ.pdf","મહાભારતની જીવનકથાઓ.pdf","માનવ-સંબંધો.pdf","મારા અનુભવો.pdf","મારા ઉપકારકો.pdf","મારી બાયપાસ સર્જરી.pdf","મોરિશિયસ અને દુબઈનો પ્રવાસ.pdf","યુદ્ધ અને યુદ્ધનેતા.pdf","રામાયણનું ચિંતન.pdf","રાષ્ટ્રીય તીર્થ આંદામાન.pdf","વસ્તુપાલ અને તેજપાલ.pdf","વાલ્મીકિ-રામાયણ-સાર.pdf","વાસ્તવિકતા.pdf","વિષ્ણુસહસ્રનામ ભાગ ૨.pdf","વિષ્ણુસહસ્રનામ.pdf","વેદાંત-સમિક્ષા.pdf","શિવાજીની શૌર્યગાથા.pdf","શું ઈશ્વર અવતાર લે છે.pdf","સંતચરિત્રો અને ચિંતન.pdf","સાચા મહાપુરુષો.pdf","સિખ (શીખ) ધર્મના પથમાં.pdf","સોરાષ્ટ્રનો મધપૂડો.pdf","સૌરાષ્ટ્રનું શૂરાતન.pdf","સ્થાપત્ય અને શૌર્યની ભૂમિ રાજસ્થાન.pdf","ચીન–મારી નજરે.pdf","'મહાભારત'નું ચિંતન.pdf"]}
""")
live_list = set(live_data.get("fileNames", []))

only_repo = repo_list - live_list
only_live = live_list - repo_list

lines = []
lines.append("=== VERIFY: App vs Server book list (sirf verify) ===")
lines.append("")
lines.append("App: Koi bundled list nahi. App runtime pe SERVER se books_server_list.json fetch karta hai.")
lines.append("     (Fallback assets/books_server_list.json repo me hai hi nahi.)")
lines.append("")
lines.append("Server (repo) public/books_server_list.json: " + str(len(repo_list)) + " books")
lines.append("Live server (URL se abhi fetch): " + str(len(live_list)) + " books")
lines.append("")
lines.append("--- Repo list me hai, LIVE (app ko jo milta hai) me NAHI: " + str(len(only_repo)))
for x in sorted(only_repo):
    lines.append("  " + x)
lines.append("")
lines.append("--- Live list me hai, REPO me NAHI: " + str(len(only_live)))
for x in sorted(only_live):
    lines.append("  " + x)

with open("verify_app_vs_server_result.txt", "w", encoding="utf-8") as out:
    out.write("\n".join(lines))
print("Written verify_app_vs_server_result.txt")
