package com.recipebookpro.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import com.recipebookpro.presentation.ui.LocaleHelper;

import java.util.ArrayList;
import java.util.List;

public class HealthWarningCache {

    private static final String PREF_NAME = "HealthWarningCachePrefsV6";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private final Context appContext;
    private final SharedPreferences prefs;

    public static class CachedWarning {
        public final boolean isSafe;
        public final String rationale;
        public final List<String> riskyIngredients;
        public final long timestamp;

        public CachedWarning(boolean isSafe, String rationale, List<String> riskyIngredients, long timestamp) {
            this.isSafe = isSafe;
            this.rationale = rationale;
            this.riskyIngredients = riskyIngredients != null ? riskyIngredients : new ArrayList<>();
            this.timestamp = timestamp;
        }
    }

    public HealthWarningCache(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Cache key includes the device locale so that a switch from Turkish→English
     * (or vice versa) always triggers a fresh AI evaluation in the correct language.
     */
    private String buildKey(String recipeId, String userProfileHash) {
        String lang = LocaleHelper.getLanguage(appContext);
        return recipeId + "_" + lang + "_" + (userProfileHash != null ? userProfileHash : "default");
    }

    public void saveWarningToCache(String recipeId, String userProfileHash, boolean isSafe, String rationale, List<String> riskyIngredients) {
        if (TextUtils.isEmpty(recipeId)) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("isSafe", isSafe);
            obj.put("rationale", rationale != null ? rationale : "");
            obj.put("timestamp", System.currentTimeMillis());

            if (riskyIngredients != null && !riskyIngredients.isEmpty()) {
                JSONArray arr = new JSONArray();
                for (String item : riskyIngredients) {
                    arr.put(item);
                }
                obj.put("riskyIngredients", arr);
            }

            prefs.edit().putString(buildKey(recipeId, userProfileHash), obj.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Profil değişince eski uyarıları temizle (24 saatlik önbellek). */
    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public CachedWarning getCachedWarning(String recipeId, String userProfileHash) {
        if (TextUtils.isEmpty(recipeId)) return null;
        String json = prefs.getString(buildKey(recipeId, userProfileHash), null);
        if (json == null) return null;

        try {
            JSONObject obj = new JSONObject(json);
            long timestamp = obj.getLong("timestamp");
            if (System.currentTimeMillis() - timestamp < EXPIRATION_MS) {
                List<String> risky = new ArrayList<>();
                JSONArray arr = obj.optJSONArray("riskyIngredients");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        String item = arr.optString(i, "").trim();
                        if (!item.isEmpty()) risky.add(item);
                    }
                }
                return new CachedWarning(obj.getBoolean("isSafe"), obj.getString("rationale"), risky, timestamp);
            } else {
                // Expired, clear it
                prefs.edit().remove(buildKey(recipeId, userProfileHash)).apply();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
