package com.swamisachidanand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * English search query ko Gujarati titles se match karne ke liye.
 * Roman (English) type kiya to Gujarati equivalents bhi search me include.
 */
public final class SearchHelper {

    /** English (roman) -> Gujarati mappings – common terms in audio/book titles */
    private static final String[][] ROMAN_TO_GUJARATI = {
        {"bhagavad", "ભગવદ્", "ભગવદ"},
        {"bhagavadgita", "ભગવદ્ ગીતા"},
        {"gita", "ગીતા"},
        {"bhakti", "ભક્તિ"},
        {"bhajan", "ભજન"},
        {"bhagwat", "ભાગવત"},
        {"vishnu", "વિષ્ણુ"},
        {"ramayan", "રામાયણ"},
        {"ramayana", "રામાયણ"},
        {"ram", "રામ"},
        {"krishna", "કૃષ્ણ"},
        {"bhartrihari", "ભર્તૃહરિ"},
        {"shata", "શતક"},
        {"sahasranam", "સહસ્રનામ"},
        {"yatra", "યાત્રા"},
        {"pravas", "પ્રવાસ"},
        {"travel", "પ્રવાસ"},
        {"tirth", "તીર્થ"},
        {"mulakat", "મુલાકાત"},
        {"africa", "આફ્રિકા"},
        {"europe", "યુરોપ"},
        {"turkey", "ટર્કી"},
        {"egypt", "ઈજિપ્ત"},
        {"andaman", "આંદામાન"},
        {"jeevan", "જીવન"},
        {"jeevankatha", "જીવનકથા"},
        {"charitra", "ચરિત્ર"},
        {"anubhav", "અનુભવ"},
        {"bypass", "બાયપાસ"},
        {"chintan", "ચિંતન"},
        {"mahabharat", "મહાભારત"},
        {"geeta", "ગીતા"},
        {"updesh", "ઉપદેશ"},
        {"pravachan", "પ્રવચન"},
        {"sansmaran", "સંસ્મરણ", "સંસ્મરણો"},
        {"dubai", "દુબઈ"},
        {"mauritius", "મોરિશિયસ"},
        {"dharam", "ધર્મ"},
        {"dharm", "ધર્મ"},
        {"chanakya", "ચાણક્ય"},
        {"vyavahar", "વ્યવહાર"},
        {"nit", "નીતિ"},
    };

    /**
     * Returns search terms to use: original query + Gujarati equivalents if query is in Roman.
     */
    public static List<String> getSearchTerms(String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();
        String q = query.trim().toLowerCase(Locale.ROOT);
        Set<String> terms = new HashSet<>();
        terms.add(q);

        // If query contains only ASCII (Roman), add Gujarati equivalents
        boolean isRoman = q.matches("^[a-zA-Z0-9\\s]+$");
        if (isRoman) {
            for (String[] pair : ROMAN_TO_GUJARATI) {
                if (pair.length < 2) continue;
                String roman = pair[0];
                if (q.contains(roman)) {
                    for (int i = 1; i < pair.length; i++) {
                        terms.add(pair[i]);
                    }
                }
            }
            // Word-by-word: split query and add Gujarati for each word
            for (String word : q.split("\\s+")) {
                if (word.length() < 2) continue;
                for (String[] pair : ROMAN_TO_GUJARATI) {
                    if (pair.length < 2) continue;
                    if (word.equals(pair[0]) || pair[0].contains(word) || word.contains(pair[0])) {
                        for (int i = 1; i < pair.length; i++) {
                            terms.add(pair[i]);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(terms);
    }

    /**
     * Returns true if text (e.g. Gujarati title) matches any of the search terms.
     */
    public static boolean matches(String text, String query) {
        if (text == null || text.isEmpty()) return false;
        String textLower = text.toLowerCase(Locale.ROOT);
        List<String> terms = getSearchTerms(query);
        for (String term : terms) {
            if (term != null && !term.isEmpty() && textLower.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
