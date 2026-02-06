package com.swamisachidanand;

import java.util.HashMap;
import java.util.Map;

public class BookTransliterator {
    
    private static final Map<String, String> GUJARATI_TO_ENGLISH = new HashMap<>();
    
    static {
        // Common Gujarati words to English transliteration
        GUJARATI_TO_ENGLISH.put("પૂર્વ", "purv");
        GUJARATI_TO_ENGLISH.put("અમરકંટક", "amarkantak");
        GUJARATI_TO_ENGLISH.put("આફ્રિકા", "africa");
        GUJARATI_TO_ENGLISH.put("યાત્રા", "yatra");
        GUJARATI_TO_ENGLISH.put("પ્રવાસ", "pravas");
        GUJARATI_TO_ENGLISH.put("ભક્તિ", "bhakti");
        GUJARATI_TO_ENGLISH.put("ઉપદેશ", "updesh");
        GUJARATI_TO_ENGLISH.put("જીવન", "jeevan");
        GUJARATI_TO_ENGLISH.put("યુરોપ", "europe");
        GUJARATI_TO_ENGLISH.put("ટર્કી", "turkey");
        GUJARATI_TO_ENGLISH.put("ઈજિપ્ત", "egypt");
        GUJARATI_TO_ENGLISH.put("આંદામાન", "andaman");
        GUJARATI_TO_ENGLISH.put("તીર્થ", "tirth");
        GUJARATI_TO_ENGLISH.put("ભાગવત", "bhagwat");
        GUJARATI_TO_ENGLISH.put("રામાયણ", "ramayan");
        GUJARATI_TO_ENGLISH.put("કૃષ્ણ", "krishna");
        GUJARATI_TO_ENGLISH.put("વિષ્ણુ", "vishnu");
        GUJARATI_TO_ENGLISH.put("ચરિત્ર", "charitra");
        
        // Add more common words as needed
    }
    
    /**
     * Transliterate Gujarati text to English (romanized)
     * This is a simple mapping-based approach
     */
    public static String transliterate(String gujaratiText) {
        if (gujaratiText == null || gujaratiText.isEmpty()) {
            return "";
        }
        
        String english = gujaratiText.toLowerCase();
        
        // Replace known Gujarati words with English
        for (Map.Entry<String, String> entry : GUJARATI_TO_ENGLISH.entrySet()) {
            english = english.replace(entry.getKey().toLowerCase(), entry.getValue());
        }
        
        // Remove Gujarati characters and keep only English/numbers
        // Keep spaces and common punctuation
        english = english.replaceAll("[^a-z0-9\\s\\-]", " ").trim();
        english = english.replaceAll("\\s+", " ");
        
        return english;
    }
    
    /**
     * Generate searchable text combining original and transliterated
     */
    public static String getSearchableText(String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return "";
        }
        
        String transliterated = transliterate(originalText);
        String original = originalText.toLowerCase();
        
        // Combine original + transliterated for better search
        if (!transliterated.isEmpty() && !transliterated.equals(original)) {
            return original + " " + transliterated;
        }
        
        return original;
    }
    
    /**
     * Quick transliteration for common patterns
     */
    public static String quickTransliterate(String text) {
        if (text == null) return "";
        
        String lower = text.toLowerCase();
        
        // Common patterns
        lower = lower.replace("પૂર્વ", "purv");
        lower = lower.replace("અમરકંટક", "amarkantak");
        lower = lower.replace("આફ્રિકા", "africa");
        lower = lower.replace("યાત્રા", "yatra");
        lower = lower.replace("પ્રવાસ", "pravas");
        lower = lower.replace("ભક્તિ", "bhakti");
        lower = lower.replace("ઉપદેશ", "updesh");
        lower = lower.replace("જીવન", "jeevan");
        
        return lower;
    }
}

