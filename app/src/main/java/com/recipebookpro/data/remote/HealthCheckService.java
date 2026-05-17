package com.recipebookpro.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.recipebookpro.BuildConfig;
import com.recipebookpro.domain.model.Recipe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Groq AI (LLaMA 3.3) kullanarak tarif güvenlik analizi.
 *
 * Mimari felsefe — "AI tek karar verici":
 *   - Tüm semantik eşleştirme, doğal dil anlama ve iki dil desteği AI'ya bırakılır.
 *   - API erişilemezse:
 *       1. Otomatik retry (rate limit vb. için)
 *       2. Profil oluşturulurken AI'ın ürettiği healthTriggers ile minimal fallback
 *   - Her seferinde sıfırdan analiz yapar, önbellek/geçmiş konuşma kullanmaz.
 */
public class HealthCheckService {

    private static final String TAG        = "HealthCheckService";
    private static final String API_KEY    = BuildConfig.GROQ_API_KEY;
    private static final String API_URL    = "https://api.groq.com/openai/v1/chat/completions";
    private static final String CHAT_MODEL = "llama-3.1-8b-instant";
    private static final int    MAX_RETRIES = 2;
    private static final long   RETRY_DELAY_MS = 2500; // 2.5 saniye bekleme

    public interface HealthCheckCallback {
        void onResult(boolean isSafe, String rationale, List<String> riskyIngredients);
        void onError(String errorMessage);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Giriş noktası
    // ─────────────────────────────────────────────────────────────────────────
    public void checkRecipeSafety(Recipe recipe,
                                   List<String> healthConditions,
                                   List<String> customHealthConditions,
                                   List<String> allergens,
                                   Map<String, List<String>> healthTriggers,
                                   String uiLangCode,
                                   HealthCheckCallback callback) {

        final Handler mainHandler = new Handler(Looper.getMainLooper());

        if (TextUtils.isEmpty(API_KEY) || "YOUR_GROQ_API_KEY".equals(API_KEY)) {
            mainHandler.post(() -> callback.onError("Groq API Key eksik"));
            return;
        }

        // Tüm koşulları tek listede topla
        List<String> allConditions = new ArrayList<>();
        if (healthConditions != null) {
            for (String cond : healthConditions) {
                String mapped = cond;
                if ("diabetes".equalsIgnoreCase(cond))        mapped = "Diyabet / Şeker Hastalığı (Diabetes)";
                else if ("kidney_disease".equalsIgnoreCase(cond)) mapped = "Böbrek Hastalığı (Kidney Disease)";
                else if ("cardiovascular".equalsIgnoreCase(cond)) mapped = "Kalp ve Damar Hastalığı (Cardiovascular)";
                else if ("hypertension".equalsIgnoreCase(cond))   mapped = "Hipertansiyon / Tansiyon (Hypertension)";
                else if ("celiac".equalsIgnoreCase(cond))         mapped = "Çölyak / Gluten Hassasiyeti (Celiac)";
                else if ("ibs".equalsIgnoreCase(cond))            mapped = "Hassas Bağırsak Sendromu (IBS)";
                allConditions.add(mapped);
            }
        }
        if (customHealthConditions != null) allConditions.addAll(customHealthConditions);
        if (allergens != null)              allConditions.addAll(allergens);

        if (allConditions.isEmpty()) {
            mainHandler.post(() -> callback.onResult(true, "", new ArrayList<>()));
            return;
        }

        boolean isTurkish = uiLangCode != null
                && uiLangCode.toLowerCase(Locale.ROOT).startsWith("tr");
        String targetLang = isTurkish ? "Turkish" : "English";

        // healthTriggers'ı ek bağlam olarak AI'ya gönder
        List<String> allTriggers = new ArrayList<>();
        if (healthTriggers != null) {
            for (Map.Entry<String, List<String>> e : healthTriggers.entrySet()) {
                if (e.getValue() != null) allTriggers.addAll(e.getValue());
            }
        }

        // Prompt oluştur
        String systemPrompt = buildSystemPrompt(targetLang);
        String userPrompt   = buildUserPrompt(recipe, allConditions, allTriggers, isTurkish);

        Log.d("GROQ_TEST", "════════ HealthCheckService ════════");
        Log.d("GROQ_TEST", "healthConditions      = " + healthConditions);
        Log.d("GROQ_TEST", "customHealthConditions= " + customHealthConditions);
        Log.d("GROQ_TEST", "allergens (legacy)    = " + allergens);
        Log.d("GROQ_TEST", "healthTriggers keys   = " + (healthTriggers != null ? healthTriggers.keySet() : "null"));
        Log.d("GROQ_TEST", "allConditions (merged)= " + allConditions);
        Log.d("GROQ_TEST", "allTriggers           = " + allTriggers);
        Log.d("GROQ_TEST", "── USER PROMPT ──\n" + userPrompt);
        Log.d("GROQ_TEST", "════════════════════════════════════");

        new Thread(() -> {
            // Retry mekanizmalı API çağrısı
            Exception lastException = null;
            int lastHttpCode = -1;
            String lastErrBody = "";

            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                if (attempt > 0) {
                    Log.d(TAG, "Retry attempt " + (attempt + 1) + "/" + MAX_RETRIES
                            + " (waiting " + RETRY_DELAY_MS + "ms)");
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ignored) {}
                }

                try {
                    JSONObject payload = new JSONObject();
                    payload.put("model", CHAT_MODEL);

                    JSONArray messages = new JSONArray();
                    JSONObject sysMsg = new JSONObject();
                    sysMsg.put("role", "system");
                    sysMsg.put("content", systemPrompt);
                    messages.put(sysMsg);

                    JSONObject userMsg = new JSONObject();
                    userMsg.put("role", "user");
                    userMsg.put("content", userPrompt);
                    messages.put(userMsg);

                    payload.put("messages", messages);

                    JSONObject responseFormat = new JSONObject();
                    responseFormat.put("type", "json_object");
                    payload.put("response_format", responseFormat);
                    payload.put("temperature", 0.0);

                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(20000);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        String responseStr = readStream(conn.getInputStream());
                        JSONObject jsonResponse = new JSONObject(responseStr);
                        String text = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        String cleanedText = cleanJson(text);
                        JSONObject resObj  = new JSONObject(cleanedText);

                        // ── isSafe parse ──
                        boolean isSafe;
                        if (resObj.has("uygun_mu")) {
                            isSafe = resObj.optBoolean("uygun_mu", true);
                        } else if (resObj.has("guvenli_mi")) {
                            isSafe = resObj.optBoolean("guvenli_mi", true);
                        } else if (resObj.has("is_safe")) {
                            isSafe = resObj.optBoolean("is_safe", true);
                        } else if (resObj.has("safe")) {
                            isSafe = resObj.optBoolean("safe", true);
                        } else {
                            isSafe = true;
                        }

                        // ── Mesaj parse ──
                        String userMessage = resObj.optString("uyari_mesaji",
                                resObj.optString("kullanici_mesaji",
                                resObj.optString("warning_message",
                                resObj.optString("message", ""))));

                        // ── Riskli malzemeler parse ──
                        List<String> riskyIngredients = new ArrayList<>();
                        JSONArray riskyArr = resObj.optJSONArray("tespit_edilen_riskli_malzemeler");
                        if (riskyArr == null) riskyArr = resObj.optJSONArray("riskli_malzemeler");
                        if (riskyArr == null) riskyArr = resObj.optJSONArray("risky_ingredients");
                        if (riskyArr == null) riskyArr = resObj.optJSONArray("detected_risky_ingredients");
                        if (riskyArr != null) {
                            for (int i = 0; i < riskyArr.length(); i++) {
                                String item = riskyArr.optString(i, "").trim();
                                if (!item.isEmpty()) riskyIngredients.add(item);
                            }
                        }

                        Log.d(TAG, "Groq analiz OK — uygun=" + isSafe
                                + " riskli=" + riskyIngredients);

                        final boolean      fSafe  = isSafe;
                        final String       fMsg   = userMessage;
                        final List<String> fRisky = new ArrayList<>(riskyIngredients);
                        mainHandler.post(() -> callback.onResult(fSafe, fMsg, fRisky));
                        return; // Başarılı — döngüden çık

                    } else {
                        lastHttpCode = responseCode;
                        try { lastErrBody = readStream(conn.getErrorStream()); } catch (Exception ignored) {}
                        Log.w(TAG, "Groq HTTP " + responseCode + " (attempt " + (attempt + 1) + "): " + lastErrBody);

                        // 429 (rate limit) veya 5xx (server error) → retry
                        if (responseCode == 429 || responseCode >= 500) {
                            continue; // sonraki denemeye geç
                        }
                        // Diğer hatalar (401, 400 vb.) → retry'sız fallback'e geç
                        break;
                    }

                } catch (Exception e) {
                    lastException = e;
                    Log.w(TAG, "Groq exception (attempt " + (attempt + 1) + ")", e);
                    // Ağ hatası → retry
                    continue;
                }
            }

            // ── Tüm retry'ler başarısız — minimal trigger-tabanlı fallback ──
            // Bu fallback sadece profil oluşturulurken AI'ın ürettiği healthTriggers'ı kullanır.
            // Hardcoded kural DEĞİL — tetikleyiciler zaten Groq tarafından analiz edilmiş.
            Log.w(TAG, "All retries failed (HTTP=" + lastHttpCode + ") → trigger-based fallback");

            if (!allTriggers.isEmpty()) {
                List<String> fallbackRisky = matchTriggersToIngredients(recipe, allTriggers);
                if (!fallbackRisky.isEmpty()) {
                    String fallbackMsg = isTurkish
                            ? "⚠ AI analizi şu an yapılamadı. Profilinize göre bazı malzemeler dikkat gerektirebilir: "
                                + android.text.TextUtils.join(", ", fallbackRisky)
                            : "⚠ AI analysis is currently unavailable. Based on your profile, some ingredients may require caution: "
                                + android.text.TextUtils.join(", ", fallbackRisky);
                    mainHandler.post(() -> callback.onResult(false, fallbackMsg, fallbackRisky));
                    return;
                }
            }

            // Trigger yoksa veya eşleşme yoksa → güvenli varsay ama not düş
            String safeMsg = isTurkish
                    ? "AI analizi geçici olarak yapılamadı. Tarif güvenli görünüyor ancak emin olmak için tekrar kontrol edebilirsiniz."
                    : "AI analysis is temporarily unavailable. The recipe appears safe, but you can check again to be sure.";
            mainHandler.post(() -> callback.onResult(true, safeMsg, new ArrayList<>()));

        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Minimal trigger-tabanlı fallback
    // healthTriggers zaten profil oluşturulurken Groq tarafından analiz edilmiş.
    // Hardcoded kural DEĞİL — AI'ın ürettiği tetikleyiciler kullanılıyor.
    // ─────────────────────────────────────────────────────────────────────────
    private List<String> matchTriggersToIngredients(Recipe recipe, List<String> triggers) {
        List<String> risky = new ArrayList<>();
        if (recipe.getIngredients() == null) return risky;

        for (Recipe.Ingredient ing : recipe.getIngredients()) {
            String ingText = (ing.getName() + " " + ing.getDisplayName() + " " + ing.getDisplayText())
                    .toLowerCase(Locale.ROOT);

            for (String trigger : triggers) {
                String tl = trigger.toLowerCase(Locale.ROOT).trim();
                if (tl.length() >= 2 && ingText.contains(tl)) {
                    String label = !TextUtils.isEmpty(ing.getDisplayName())
                            ? ing.getDisplayName().trim()
                            : ing.getName().trim();
                    if (!label.isEmpty() && !risky.contains(label)) {
                        risky.add(label);
                    }
                    break;
                }
            }
        }
        return risky;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // System Prompt — AI'ın tüm analiz mantığı burada
    // ─────────────────────────────────────────────────────────────────────────
    private String buildSystemPrompt(String uiLangCode) {
        boolean isTurkish = uiLangCode.toLowerCase(Locale.ROOT).startsWith("tr");

        if (isTurkish) {
            return "Sen dünyanın en titiz sağlık ve gastronomi güvenlik asistanısın.\n\n" +
                "# KRİTİK KURAL\n" +
                "1. SADECE 'GÜNCEL PROFİL' kısmında yazan hastalıklara/alerjilere göre analiz yap.\n" +
                "2. Profilde YAZMAYAN hiçbir hastalık için ASLA uyarı verme.\n" +
                "3. KESİNLİKLE uydurma veya tahmin yapma. Malzemede açıkça belirtilmedikçe 'çapraz bulaşma' (cross-contamination) veya 'üretim bandı' gibi varsayımlarda BULUNMA.\n" +
                "4. Bir malzeme DOĞRUDAN alerjenin kendisi veya türevi değilse, o malzemeyi GÜVENLİ kabul etmek ZORUNDASIN. Alakasız şeyleri tehlikeli sayma.\n\n" +
                "# ÇIKTI FORMATI — SADECE JSON\n" +
                "{\n" +
                "  \"uygun_mu\": true/false,\n" +
                "  \"tespit_edilen_riskli_malzemeler\": [\"malzeme1\"],\n" +
                "  \"uyari_mesaji\": \"Neden riskli olduğunu açıklayan kısa ve net TÜRKÇE uyarı.\"\n" +
                "}\n\n" +
                "KURAL: JSON ANAHTARLARI HER ZAMAN TÜRKÇE OLMALI.\n" +
                "KURAL: TÜM metin değerleri TÜRKÇE olmalı.\n" +
                "KURAL: JSON dışında hiçbir şey yazma.";
        } else {
            return "You are the world's most meticulous health and culinary safety assistant.\n\n" +
                "# CRITICAL RULE\n" +
                "1. ONLY analyze based on the conditions listed in the 'CURRENT PROFILE'.\n" +
                "2. NEVER give warnings for diseases or allergies NOT listed in the profile.\n" +
                "3. DO NOT hallucinate or guess. DO NOT assume 'cross-contamination' or 'shared processing facilities' unless explicitly stated in the ingredient.\n" +
                "4. If an ingredient is NOT clearly and directly the allergen itself (or a known direct derivative), you MUST assume it is safe. Do not flag unrelated items.\n\n" +
                "# OUTPUT FORMAT — ONLY JSON\n" +
                "{\n" +
                "  \"uygun_mu\": true/false,\n" +
                "  \"tespit_edilen_riskli_malzemeler\": [\"ingredient1\"],\n" +
                "  \"uyari_mesaji\": \"A clear and short warning in ENGLISH explaining the risk.\"\n" +
                "}\n\n" +
                "RULE: JSON KEYS MUST ALWAYS BE IN TURKISH (uygun_mu, tespit_edilen_riskli_malzemeler, uyari_mesaji).\n" +
                "RULE: ALL text values MUST be in ENGLISH.\n" +
                "RULE: Do NOT write anything outside the JSON.";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User Prompt
    // ─────────────────────────────────────────────────────────────────────────
    private String buildUserPrompt(Recipe recipe, List<String> allConditions,
                                    List<String> allTriggers, boolean isTurkish) {
        StringBuilder sb = new StringBuilder();

        if (isTurkish) {
            sb.append("# GÜNCEL PROFİL\n");
            sb.append("Hastalıklar ve Alerjiler:\n");
            for (String cond : allConditions) {
                sb.append("  - ").append(cond).append("\n");
            }
            if (!allTriggers.isEmpty()) {
                sb.append("\nÖnceden tespit edilen riskli maddeler (referans):\n");
                for (String t : allTriggers) {
                    sb.append("  - ").append(t).append("\n");
                }
            }
            sb.append("\n# DİKKAT EDİLECEKLER (Semantik Eşleşme)\n");
            sb.append("Eğer profildeki hastalıklarla ilgiliyse şu gizli malzemelere de dikkat et:\n");
            String condsLower = allConditions.toString().toLowerCase(Locale.ROOT);
            if (condsLower.contains("şeker") || condsLower.contains("diyabet")) {
                sb.append("  - Şeker/Diyabet için: bal, pekmez, şurup, reçel, şeker\n");
            }
            if (condsLower.contains("çölyak") || condsLower.contains("gluten")) {
                sb.append("  - Çölyak/Gluten için: un, buğday, irmik, makarna, ekmek\n");
            }
            if (condsLower.contains("laktoz")) {
                sb.append("  - Laktoz için: süt, peynir, tereyağı, krema, yoğurt\n");
            }
            if (condsLower.contains("kalp") || condsLower.contains("damar")) {
                sb.append("  - Kalp ve Damar için: tereyağı, krema, sucuk, sosis, pastırma, margarin\n");
            }
            if (condsLower.contains("tansiyon")) {
                sb.append("  - Tansiyon için: tuz, soya sosu\n");
            }

            sb.append("\n# TARİF\n");
            sb.append("Tarif Adı: \"").append(recipe.getTitle()).append("\"\n");
            sb.append("Malzemeler:\n");
        } else {
            sb.append("# CURRENT PROFILE\n");
            sb.append("Health Conditions & Allergies:\n");
            for (String cond : allConditions) {
                sb.append("  - ").append(cond).append("\n");
            }
            if (!allTriggers.isEmpty()) {
                sb.append("\nPreviously identified risky ingredients (reference):\n");
                for (String t : allTriggers) {
                    sb.append("  - ").append(t).append("\n");
                }
            }
            sb.append("\n# SEMANTIC MATCHING GUIDELINES\n");
            sb.append("If related to the profile, check for these hidden sources:\n");
            String condsLower = allConditions.toString().toLowerCase(Locale.ROOT);
            if (condsLower.contains("diabetes") || condsLower.contains("sugar") || condsLower.contains("şeker")) {
                sb.append("  - Diabetes/Sugar: honey, molasses, syrup, jam, sugar\n");
            }
            if (condsLower.contains("celiac") || condsLower.contains("gluten")) {
                sb.append("  - Celiac/Gluten: flour, wheat, semolina, pasta, bread\n");
            }
            if (condsLower.contains("lactose") || condsLower.contains("dairy")) {
                sb.append("  - Lactose/Dairy: milk, cheese, butter, cream, yogurt\n");
            }
            if (condsLower.contains("cardio") || condsLower.contains("heart") || condsLower.contains("kalp") || condsLower.contains("damar")) {
                sb.append("  - Cardiovascular/Heart: butter, cream, sausage, bacon, margarine\n");
            }
            if (condsLower.contains("hypertension") || condsLower.contains("blood pressure") || condsLower.contains("tansiyon")) {
                sb.append("  - Hypertension: salt, soy sauce\n");
            }

            sb.append("\n# RECIPE\n");
            sb.append("Recipe Name: \"").append(recipe.getTitle()).append("\"\n");
            sb.append("Ingredients:\n");
        }

        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            for (Recipe.Ingredient ing : recipe.getIngredients()) {
                String line = ing.getDisplayText().trim();
                if (!line.isEmpty()) sb.append("  - ").append(line).append("\n");
            }
        } else {
            sb.append(recipe.getFormattedIngredients()).append("\n");
        }

        sb.append("\nAnaliz et. Sadece profilde yazanlara göre değerlendir.");

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Yardımcı
    // ─────────────────────────────────────────────────────────────────────────
    private String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line.trim());
        return sb.toString();
    }

    private String cleanJson(String text) {
        String s = text.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        if (s.startsWith("```"))     s = s.substring(3);
        if (s.endsWith("```"))       s = s.substring(0, s.length() - 3);
        return s.trim();
    }
}
