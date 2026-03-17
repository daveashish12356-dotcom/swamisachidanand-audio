package com.swamisachidanand;

import java.util.Arrays;
import java.util.List;

/**
 * Shared category filter for Book Store, Books (PDF) and Audio pages.
 * Same labels, ids and name-based matching so "All" list is filtered by selected category.
 */
public final class BookStoreCategoryHelper {

    private static final String[] FILTER_LABELS = {
        "All", "📖 નવાં પુસ્તકો", "🛞 અધ્યાત્મ-દર્શન", "🕉️ ધર્મ-દર્શન", "👁️ સમાજ-દર્શન",
        "👁️ ચરિત્ર-દર્શન", "રાષ્ટ્ર-દર્શન", "👤 અંગત-દર્શન", "📚 ગ્રંથ-દર્શન", "ભારતપ્રવાસ-દર્શન",
        "🕉️ ભારત-તીર્થ-દર્શન", "✈️ વિદેશ પ્રવાસ", "📚 સંદર્ભ સાહિત્ય", "📖 પ્રસંગ-દર્શન", "📚 સંપાદન-સંકલન"
    };

    private static final String[] FILTER_IDS = {
        "all", "new", "adhyatm", "dharm", "samaj", "charitr", "rashtra", "angat", "granth",
        "pravas", "tirth", "videsh", "sandarbh", "prasang", "sampadan"
    };

    public static String[] getFilterLabels() {
        return FILTER_LABELS.clone();
    }

    public static String[] getFilterIds() {
        return FILTER_IDS.clone();
    }

    /** Same as Book Store: બધાં + all category chips, no લોકપ્રિય. For Books & Audio pages. */
    public static List<String> getFilterLabelsForBooks() {
        List<String> list = new java.util.ArrayList<>();
        list.add("બધાં");
        for (int i = 1; i < FILTER_LABELS.length; i++) list.add(FILTER_LABELS[i]);
        return list;
    }

    public static List<String> getFilterIdsForBooks() {
        List<String> list = new java.util.ArrayList<>(Arrays.asList(FILTER_IDS));
        return list;
    }

    @Deprecated
    /** Use getFilterLabelsForBooks() for Books/Audio (no લોકપ્રિય). */
    public static List<String> getFilterLabelsWithPopular() {
        List<String> list = new java.util.ArrayList<>(getFilterLabelsForBooks());
        list.add("લોકપ્રિય");
        return list;
    }

    @Deprecated
    /** Use getFilterIdsForBooks() for Books/Audio. */
    public static List<String> getFilterIdsWithPopular() {
        List<String> list = new java.util.ArrayList<>(getFilterIdsForBooks());
        list.add("popular");
        return list;
    }

    /**
     * Returns true if the given book/title name belongs to the category (by keyword match).
     * Use for PDF book name or audio book title.
     */
    public static boolean belongsToCategory(String name, String categoryId) {
        if (name == null || categoryId == null || "all".equals(categoryId)) return false;
        String n = name.trim();
        if ("new".equals(categoryId)) {
            return n.contains("મહાન રામાનુજાચાર્ય") || n.contains("દેવાલય થી દેહાલય");
        }
        if ("adhyatm".equals(categoryId)) {
            String[] keys = { "ભારતીય દર્શનો", "શિવતત્વ", "પ્રવચનમંગલ", "શ્રીકૃષ્ણલીલારહસ્ય", "વેદાન્ત-સમીક્ષા", "ચાલો", "અભિગમ બદલીએ", "શું ઇશ્વર અવતાર", "હવે તો જાગીએ", "અગવડોમાં આરાધના", "સિખ", "ધર્મના પક્ષમાં", "ત્યાગ-અહિંસા", "મહાભારત-સાર", "માનસ મંગલાચરણ", "ખોટા ઉપદેશો", "ઉપસંહાર", "નવા વિચારો", "નવી દિશા", "નવી આશા", "આપણી દુર્બળતાઓ", "વિચારોનો ગુલદસ્તો", "છેડતીથી બરબાદી" };
            if (n.equals("ધર્મ")) return true;
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("dharm".equals(categoryId)) {
            String[] keys = { "વિષ્ણુસહસ્ત્રનામ", "હનુમાન ચાલીસા" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("samaj".equals(categoryId)) {
            String[] keys = { "અધોગતિનું મૂળ", "આપણે અને સમાજ", "સંસાર-રામાયણ", "નવી દ્રષ્ટી", "નવી દૃષ્ટિ", "નર-નારીના સંબંધો", "માનવ-સંબંધો", "પત્નીઓથી થતી પીડા", "વાસ્તવિકતા" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("charitr".equals(categoryId)) {
            String[] keys = { "શિવાજીની શૌર્યગાથા", "સિકંદર અને નેપોલિયન", "સરદાર સાહેબ", "સરદાર પટેલ", "વડાપ્રધાન બન્યા", "મહર્ષિ દયાનંદ સરસ્વતી", "ફાંસીના વરરાજાઓ", "સંતચરિત્રો", "સૌરાષ્ટ્રનો મધપૂડો", "શહીદોની ક્રાંતિગાથાઓ", "ક્રાંતિકથાઓ", "સૌરાષ્ટ્રનું શૂરાતન", "મહાન મહિલાઓ", "સાચા મહાપુરુષો", "વીરતા પરમો ધર્મ", "વસ્તુપાળ અને તેજપાળ", "બુદ્ધ ચરિત્ર ચિંતન", "મહાદેવી અહલ્યાબાઈ", "રામમોહન રાય", "ઋષિવર શ્રી પ્રભાશંકર", "સિંધુતાઈ સપકાળ", "કચ્છી કથાઓ", "વીરાંગના મલાલા", "કબૂતરો, આંખો ઉઘાડો", "કાલાપાની", "ગોંડલ-બાપુ", "ગોંડલ બાપુ", "ભગવતસિંહજી", "મહાન રામાનુજાચાર્ય" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("rashtra".equals(categoryId)) {
            String[] keys = { "રાષ્ટ્રના સળગતા પ્રશ્નો", "કોલંબસ અને વાસ્કો", "મહાન લિંકન", "કાશ્મીરનો ટૂંકો ઇતિહાસ", "મોગલોનો આંતરિક", "ઇઝરાયલનાં ચમત્કારિક" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("angat".equals(categoryId)) {
            String[] keys = { "મારા અનુભવો", "મારા પૂર્વાશ્રમનાં સંસ્મરણો", "મારી બાયપાસ સર્જરી", "મારા ઉપકારકો", "કડવામીઠા અનુભવો", "અમારા વૃદ્ધાશ્રમોના કડવા અનુભવો", "વિદેશયાત્રાના પ્રેરક પ્રસંગો" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("granth".equals(categoryId)) {
            String[] keys = { "કાન્હડદેપ્રબંધ", "કાન્હડદે પ્રબંધ", "ગંગાસતીની અમર વાણી", "ભર્તૃહરિનાં બે શતકો", "વિદુર નિતિ", "વિદુરનીતિ", "ચાણક્યની વ્યવહારનિતિ", "ચાણક્યની રાજનીતિ", "કબીરજીનું ચિંતન", "મુનશી પ્રેમચંદજીનો પુનરાવતાર", "વાલ્મીકી-રામાયણ-સાર", "રામાયણનું ચિંતન", "માનસ મધપૂડો", "બુદ્ધ-જાતક-ચિંતન", "ભાગવતનું ચિંતન", "મહાભારતનું ચિંતન", "ગીતાજીનું ચિંતન", "ગીતા અને આપણા પ્રશ્નો", "પ્રશ્નોના મૂળમાં" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("pravas".equals(categoryId)) {
            String[] keys = { "પશ્ચિમ બંગાળ મારી નજરે", "હિમાલયના હીંડોળે", "સ્થાપત્ય અને શૌર્યની ભૂમિ રાજસ્થાન", "લેહ, લદાખ", "પૂર્વની સાત બહેનો", "ગોવાપ્રવાસ", "કૌસાની", "રાણીખેત", "નૈનિતાલ", "રાષ્ટ્રીય તીર્થ આંદામાન", "આંદામાનનો પ્રવાસ", "લક્ષદ્વિપ-પ્રવાસ", "હમ્પી, વેલુર અને હળેબીડુ" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("tirth".equals(categoryId)) {
            String[] keys = { "હિમાલયના ચાર ધામ", "બોધગયામાં નેત્રશ્રાદ્ધ", "પટના અને પન્ના", "કાશી-અયોધ્યા યાત્રા", "અમારી નર્મદા", "નર્મદા પરિક્રમા", "પરિક્રમા-સંયુક્ત", "અમરકંટક અને મધ્યપ્રદેશનો મહિમા", "બીજાપુરથી નાંદેડ", "તામિલનાડુની યાત્રા", "રામાનુજ પ્રતિમા દર્શન", "હરિદ્વારની યાત્રા" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("videsh".equals(categoryId)) {
            String[] keys = { "પૃથ્વી-પ્રદક્ષિણા", "ઇજિપ્ત-ઇસ્ત્રાઇલની ઝાંખી", "ટાન્ઝાનિયામાં 17 દિવસ", "યુરોપની અટારીએથી", "દક્ષિણ આફ્રિકાની ઊડતી મુલાકાત", "પશ્ચિમ થઈને રશિયા", "ચીન-મારી નજરે", "પૂર્વમાં નવું પશ્ચિમ", "આફ્રિકા-પ્રવાસનાં સંસ્મરણો", "શ્રીલંકાની સફરે", "દક્ષિણ-પૂર્વનો પ્રવાસ", "ટર્કી અને ઇજિપ્ત", "ફરી પાછા પૂર્વમાં", "સિક્કિમ અને ભુતાનનો પ્રવાસ", "પૂર્વ-યુરોપનો પ્રવાસ", "મોરેશિયસ અને દુબઇનો પ્રવાસ", "નેપાળ-યાત્રા", "આપણે અને પશ્ચિમ" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("sandarbh".equals(categoryId)) {
            String[] keys = { "ભારતીય યુદ્ધોનો સંક્ષિપ્ત ઇતિહાસ", "ભારતમાં અંગ્રેજોનાં યુદ્ધો", "યુદ્ધ અને યુદ્ધનેતા", "જૂનાગઢનો આઝાદીજંગ", "તનોટમાતા અને લોંગોવાલનું યુદ્ધ" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("prasang".equals(categoryId)) {
            String[] keys = { "મહાભારતની જીવનકથાઓ", "પૌરાણિક કથાઓ", "ઉપનિષદોની કથાઓ અને ચિંતન", "આપણી બોધકથાઓ" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        if ("sampadan".equals(categoryId)) {
            String[] keys = { "પરિવર્તનને પંથે", "પ્રશ્ન એ જ ઉત્તર", "ચિંતનકણિકાઓ", "ગુરુ નહિ, માર્ગદર્શક" };
            for (String k : keys) if (n.contains(k)) return true;
            return false;
        }
        return false;
    }

    private BookStoreCategoryHelper() {}
}
