package com.swamisachidanand;

/**
 * Daily pravachan folder titles. Server-backed categories: see {@link DainikPravachanServer} (GitHub public/audio_pravachan/).
 */
public final class DainikPravachanCategories {
    private DainikPravachanCategories() {}

    /** Display order = folder names (last segment of source paths). */
    public static final String[] TITLES = {
            "માનસગંગા",
            "મારા_અનુભવો",
            "યોગસૂત્ર",
            "રામાયણ તુલના",
            "રામાયણ સમીક્ષા",
            "વાલ્મિકી રામાયણ",
            "વિશ્વંભરી સ્તુતિ",
            "વૈદિક_ધર્મ,_લૉસ_એન્જલસ",
            "શ્રીકૃષ્ણ ચરિત્ર",
            "શ્રીમદ્ ભાગવત કથા",
            "સંક્ષિપ્ત_રામચરિત_માનસ",
            "સંતચરિત્ર",
            "સ્વાધ્યાય_પ્રવચનો",
            "હિન્દુધર્મનાં_પ્રતિકો,_પોર્ટલેન્ડ_ઓરેગાંવ",
            "હ્યુસ્ટન_ટેક્ષાસમાં_પ્રવચનો",
            "ઉપનિષદો",
            "ગીતા",
            "ગીતા_સ્વાધ્યાય,_આનંદધારા_આશ્રમ",
            "ગીતાધર્મ",
            "તુલસીકૃત રામાયણ",
            "ધર્મ,  કેલિફોર્નિયા",
            "પંચામૃત",
            "પ્રશ્નોત્તરી,",
            "ભારતીય દર્શનો",
            "મહાભારત_ધર્મજીવન_ચર્ચા_1",
    };

    /** Distinct accent colors (cycle) — matches “colored category” look on cards. */
    private static final int[] ACCENT_COLORS = {
            0xFFE65100,
            0xFFF9A825,
            0xFF1565C0,
            0xFFC62828,
            0xFF00897B,
            0xFF6A1B9A,
            0xFF2E7D32,
            0xFFAD1457,
            0xFF4527A0,
            0xFF00695C,
            0xFFD84315,
            0xFF0277BD,
            0xFF558B2F,
            0xFF5D4037,
            0xFF455A64,
            0xFF6D4C41,
            0xFF283593,
            0xFFBF360C,
            0xFF4A148C,
            0xFF1B5E20,
            0xFFB71C1C,
            0xFF004D40,
            0xFF3E2723,
            0xFF01579B,
            0xFF33691E,
    };

    public static int colorAt(int index) {
        if (index < 0) return ACCENT_COLORS[0];
        return ACCENT_COLORS[index % ACCENT_COLORS.length];
    }
}
