package com.recipebookpro.domain.model;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.Locale;

/**
 * Bilingual label stored in Firestore/Room ({@code tr} + {@code en}).
 */
public class LocalizedText implements Serializable {

    private String key;
    private String tr;
    private String en;

    public LocalizedText() {
    }

    public LocalizedText(String key, String tr, String en) {
        this.key = key;
        this.tr = tr;
        this.en = en;
    }

    public static LocalizedText fromLegacy(String text) {
        if (TextUtils.isEmpty(text)) {
            return new LocalizedText("", "", "");
        }
        String trimmed = text.trim();
        String key = generateKey(trimmed);
        if (looksTurkish(trimmed)) {
            return new LocalizedText(key, trimmed, null);
        }
        return new LocalizedText(key, null, trimmed);
    }

    public static String generateKey(String primary) {
        if (TextUtils.isEmpty(primary)) {
            return "condition_" + System.currentTimeMillis();
        }
        String slug = primary.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9ğüşıöçĞÜŞİÖÇ]+", "_")
                .replaceAll("^_+|_+$", "");
        if (slug.isEmpty()) {
            slug = "condition";
        }
        return "c_" + slug;
    }

    public static boolean looksTurkish(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        return text.matches(".*[ğüşıöçĞÜŞİÖÇ].*")
                || text.matches("(?i).*\\b(laktoz|süt|glüten|diyabet|çölyak|hipertansiyon|böbrek|şeker)\\b.*");
    }

    public static boolean looksEnglish(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        if (looksTurkish(text)) {
            return false;
        }
        return text.matches("(?i).*\\b(diabetes|allergy|disease|intolerance|celiac|lactose|gluten|hypertension)\\b.*")
                || text.matches("^[A-Za-z0-9\\s\\-/',.()]+$");
    }

    /** True when the stored side for this language is missing or written in the wrong language. */
    public boolean needsLangSide(String lang) {
        String normalized = lang != null && lang.toLowerCase(Locale.ROOT).startsWith("tr") ? "tr" : "en";
        if ("tr".equals(normalized)) {
            return TextUtils.isEmpty(tr) || looksEnglish(tr);
        }
        return TextUtils.isEmpty(en) || looksTurkish(en);
    }

    public String getKey() {
        return key == null ? "" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTr() {
        return tr;
    }

    public void setTr(String tr) {
        this.tr = tr;
    }

    public String getEn() {
        return en;
    }

    public void setEn(String en) {
        this.en = en;
    }

    public String getForLang(String lang) {
        if (lang != null && lang.toLowerCase(Locale.ROOT).startsWith("tr")) {
            return !TextUtils.isEmpty(tr) ? tr : fallbackEn();
        }
        return !TextUtils.isEmpty(en) ? en : fallbackTr();
    }

    private String fallbackEn() {
        return en != null ? en : "";
    }

    private String fallbackTr() {
        return tr != null ? tr : "";
    }

    public boolean needsTranslation() {
        return TextUtils.isEmpty(tr) || TextUtils.isEmpty(en);
    }

    public void mergeFrom(LocalizedText other) {
        if (other == null) {
            return;
        }
        if (TextUtils.isEmpty(key)) {
            key = other.key;
        }
        if (TextUtils.isEmpty(tr) && !TextUtils.isEmpty(other.tr)) {
            tr = other.tr;
        }
        if (TextUtils.isEmpty(en) && !TextUtils.isEmpty(other.en)) {
            en = other.en;
        }
    }
}
