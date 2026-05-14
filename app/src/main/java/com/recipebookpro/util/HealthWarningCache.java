package com.recipebookpro.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.Locale;

public class HealthWarningCache {

    private static final String PREF_NAME = "HealthWarningCachePrefs";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private final SharedPreferences prefs;

    public static class CachedWarning {
        public final boolean isSafe;
        public final String rationale;
        public final long timestamp;

        public CachedWarning(boolean isSafe, String rationale, long timestamp) {
            this.isSafe = isSafe;
            this.rationale = rationale;
            this.timestamp = timestamp;
        }
    }

    public HealthWarningCache(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Cache key includes the device locale so that a switch from Turkish→English
     * (or vice versa) always triggers a fresh AI evaluation in the correct language.
     */
    private String buildKey(String recipeId) {
        String lang = Locale.getDefault().getLanguage(); // "tr" or "en" etc.
        return recipeId + "_" + lang;
    }

    public void saveWarningToCache(String recipeId, boolean isSafe, String rationale) {
        if (TextUtils.isEmpty(recipeId)) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("isSafe", isSafe);
            obj.put("rationale", rationale != null ? rationale : "");
            obj.put("timestamp", System.currentTimeMillis());

            prefs.edit().putString(buildKey(recipeId), obj.toString()).apply();
        } catch (Exception ignored) {}
    }

    public CachedWarning getCachedWarning(String recipeId) {
        if (TextUtils.isEmpty(recipeId)) return null;
        String json = prefs.getString(buildKey(recipeId), null);
        if (json == null) return null;

        try {
            JSONObject obj = new JSONObject(json);
            long timestamp = obj.getLong("timestamp");
            if (System.currentTimeMillis() - timestamp < EXPIRATION_MS) {
                return new CachedWarning(obj.getBoolean("isSafe"), obj.getString("rationale"), timestamp);
            } else {
                // Expired, clear it
                prefs.edit().remove(buildKey(recipeId)).apply();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
