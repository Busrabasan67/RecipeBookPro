package com.recipebookpro.domain.nutrition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM veya kullanıcı metninden tahmini kalori (kcal) sayısını çıkarır.
 * Model "2,340" / "Kalori: 2340 kcal" gibi formatlar kullandığında ilk rakam grubunu (\d+) almak yanlıştır (2 kalır).
 */
public final class EstimatedCaloriesParser {

    private static final int MAX_REASONABLE_KCAL = 99_999;
    /** Kalori satırı: Kalori: 2,340 kcal vb. */
    private static final Pattern LABELED = Pattern.compile(
            "(?i)(?:kalori|calories?|energy)\\s*[:：=]\\s*([\\d\\s,.']+)");
    /** Rakam grupları; binlik ayrıcı ile birlikte olabilir. */
    private static final Pattern FLEX_NUMBER_CHUNK = Pattern.compile("\\d(?:[\\d\\s,.'\\u2019]*\\d)+|\\d{2,}");

    private EstimatedCaloriesParser() {
    }

    /**
     * @return Pozitif kcal tahmini veya bilemezse {@code absentValue} (ör. -1).
     */
    public static int parseTotalKcalFlexible(String rawText, int absentValue) {
        if (rawText == null) return absentValue;
        String t = rawText.trim();
        if (t.isEmpty()) return absentValue;

        Matcher labeled = LABELED.matcher(t);
        if (labeled.find()) {
            int v = parseLooseGroupedDigits(labeled.group(1));
            if (isPlausibleTotal(v)) return v;
        }

        String strippedKcalWord = t.replaceAll("(?i)\\s*k\\s*c\\s*a\\s*l\\s*", "").trim();

        // Model yanıtı neredeyse sadece sayı + binlik ayracı
        if (strippedKcalWord.matches("[\\d\\s,.'\\u2019]+")) {
            int v = parseLooseGroupedDigits(strippedKcalWord);
            if (isPlausibleTotal(v)) return v;
        }

        int best = absentValue;
        Matcher m = FLEX_NUMBER_CHUNK.matcher(t);
        while (m.find()) {
            int v = parseLooseGroupedDigits(m.group());
            if (isPlausibleTotal(v) && v > best) {
                best = v;
            }
        }
        // Tek haneli 0–9 dışında kalan "2340" düz dizisi chunk'ta yakalanmış olmalı;
        Matcher lone = Pattern.compile("\\d{1,6}\\b").matcher(t);
        while (lone.find()) {
            int v = parseLooseGroupedDigits(lone.group());
            if (isPlausibleTotal(v) && v > best) {
                best = v;
            }
        }
        return best;
    }

    /** Virgül/nokta/boşluk binlik ayraçlarını koruyarak rakamları birleştirir ("2.340" → 2340). */
    static int parseLooseGroupedDigits(String fragment) {
        if (fragment == null) return -1;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < fragment.length(); i++) {
            char c = fragment.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else if (isCommaLikeSeparatorChar(c)) {
                continue;
            } else if (Character.isWhitespace(c)) {
                continue;
            } else if (c == '.' && isThousandsGroupingDot(fragment, i)) {
                continue;
            } else if (c == '.') {
                break;
            } else {
                break;
            }
        }
        if (digits.length() == 0) return -1;
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static boolean isCommaLikeSeparatorChar(char c) {
        return c == ',' || c == '\'' || c == '\u2019';
    }

    /**
     * "2.340" / "11.995" için binlik nokta; "2.5" veya sonu . olan sayılar için false.
     */
    private static boolean isThousandsGroupingDot(String fragment, int dotIndex) {
        int pos = dotIndex + 1;
        int digitsAfter = 0;
        while (pos < fragment.length() && digitsAfter < 3) {
            char ch = fragment.charAt(pos);
            if (ch < '0' || ch > '9') {
                break;
            }
            digitsAfter++;
            pos++;
        }
        if (digitsAfter != 3) {
            return false;
        }
        if (pos >= fragment.length()) {
            return true;
        }
        char next = fragment.charAt(pos);
        return next < '0' || next > '9';
    }

    private static boolean isPlausibleTotal(int v) {
        return v > 0 && v <= MAX_REASONABLE_KCAL;
    }
}
