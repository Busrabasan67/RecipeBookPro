package com.recipebookpro.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.recipebookpro.BuildConfig;
import com.recipebookpro.domain.model.LocalizedText;

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

/**
 * Groq AI service for analyzing free-text health condition inputs.
 * Takes user's raw text (e.g., "gizli şekerim var") and returns:
 * - Standardized condition name
 * - Condition type (disease/allergy)
 * - List of trigger ingredients
 * - Short warning template
 */
public class GroqHealthProfileAnalyzer {

    private static final String TAG = "GroqHealthProfileAnalyzer";
    private static final String API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String CHAT_MODEL = "llama-3.1-8b-instant";

    public interface ProfileAnalysisCallback {
        void onResult(String durumTuru, LocalizedText standartIsim,
                      List<String> tetikleyiciler, LocalizedText kisaUyariSablonu);
        void onError(String errorMessage);
    }

    public void analyzeHealthInput(String userText, String uiLangCode, ProfileAnalysisCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        if (TextUtils.isEmpty(API_KEY) || "YOUR_GROQ_API_KEY".equals(API_KEY)) {
            mainHandler.post(() -> callback.onError("Groq API Key eksik"));
            return;
        }

        if (TextUtils.isEmpty(userText)) {
            mainHandler.post(() -> callback.onError("Boş girdi"));
            return;
        }

        String systemPrompt = "# ROL\n" +
                "Sen profesyonel bir tıp ve gastronomi uzmanı yapay zeka ajanısın. " +
                "Görevin, kullanıcının doğal dille (serbest metin olarak) yazdığı hastalık veya alerji durumunu analiz etmek, " +
                "bunu tıbbi/gastronomik standart bir isme kavuşturmak ve bu durumu tetikleyebilecek gizli/açık tüm besin maddelerini listelemektir.\n\n" +
                "# GÖREV\n" +
                "Aşağıdaki kullanıcı girdisini analiz et ve SADECE sana verilen JSON şablonuna uygun olarak yanıt dön. " +
                "Ekstra açıklama, selamlaşma veya markdown (kod bloğu hariç) ekleme.\n\n" +
                "# ÇIKTI FORMATI (JSON)\n" +
                "{\n" +
                "  \"durum_turu\": \"Hastalık\" veya \"Alerji\",\n" +
                "  \"standart_isim_tr\": \"Durumun Türkçe standart adı\",\n" +
                "  \"standart_isim_en\": \"Durumun İngilizce standart adı\",\n" +
                "  \"tetikleyiciler\": [\"tetikleyen malzeme 1\", \"tetikleyen malzeme 2\", ...],\n" +
                "  \"kisa_uyari_sablonu_tr\": \"Türkçe kısa uyarı cümlesi\",\n" +
                "  \"kisa_uyari_sablonu_en\": \"İngilizce kısa uyarı cümlesi\"\n" +
                "}\n\n" +
                "Both Turkish and English name/template fields are REQUIRED. " +
                "standart_isim_tr must be Turkish; standart_isim_en must be English (never copy Turkish into _en). " +
                "Trigger ingredients may be in either language but must be gastronomically accurate. " +
                "You MUST output strictly valid JSON matching this schema exactly. No extra text.";

        String uiHint = (uiLangCode != null && uiLangCode.toLowerCase().startsWith("tr"))
                ? "Kullanıcının uygulama dili: Türkçe."
                : "User app language: English.";
        String userPrompt = uiHint + "\nKullanıcı Metni: \"" + userText + "\"";

        new Thread(() -> {
            int maxRetries = 3;
            long retryDelayMs = 1500;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
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

                    payload.put("temperature", 0.1);

                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        String responseStr = readStreamAsString(conn.getInputStream());
                        JSONObject jsonResponse = new JSONObject(responseStr);
                        String text = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        String cleanedText = cleanJsonResponse(text);
                        JSONObject resObj = new JSONObject(cleanedText);

                        String durumTuru = resObj.optString("durum_turu", "Hastalık");
                        String standartTr = resObj.optString("standart_isim_tr",
                                resObj.optString("standart_isim", userText));
                        String standartEn = resObj.optString("standart_isim_en", "");
                        if (standartEn.equals(standartTr)) {
                            standartEn = "";
                        }
                        String uyariTr = resObj.optString("kisa_uyari_sablonu_tr",
                                resObj.optString("kisa_uyari_sablonu", ""));
                        String uyariEn = resObj.optString("kisa_uyari_sablonu_en", uyariTr);

                        String key = LocalizedText.generateKey(
                                !standartTr.isEmpty() ? standartTr : standartEn);
                        LocalizedText standartIsim = new LocalizedText(key, standartTr, standartEn);
                        LocalizedText kisaUyari = new LocalizedText(key + "_tpl", uyariTr, uyariEn);

                        List<String> tetikleyiciler = new ArrayList<>();
                        JSONArray tetArr = resObj.optJSONArray("tetikleyiciler");
                        if (tetArr != null) {
                            for (int i = 0; i < tetArr.length(); i++) {
                                String item = tetArr.optString(i, "").trim();
                                if (!item.isEmpty()) {
                                    tetikleyiciler.add(item);
                                }
                            }
                        }

                        mainHandler.post(() -> callback.onResult(durumTuru, standartIsim,
                                tetikleyiciler, kisaUyari));
                        return;

                    } else if (responseCode == 429 || responseCode >= 500) {
                        if (attempt == maxRetries) {
                            String errBody = readErrorBody(conn);
                            Log.w(TAG, "HTTP " + responseCode + " after retries: " + errBody);
                            mainHandler.post(() -> callback.onError("API meşgul (HTTP " + responseCode + ")"));
                            return;
                        }
                        Log.w(TAG, "HTTP " + responseCode + ", retrying... attempt " + attempt);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2;
                    } else {
                        String errBody = readErrorBody(conn);
                        Log.e(TAG, "HTTP " + responseCode + " — " + errBody);
                        mainHandler.post(() -> callback.onError("API Hatası: " + responseCode));
                        return;
                    }

                } catch (Exception e) {
                    if (attempt == maxRetries) {
                        Log.e(TAG, "Exception during health profile analysis", e);
                        mainHandler.post(() -> callback.onError(e.getMessage()));
                        return;
                    }
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        mainHandler.post(() -> callback.onError("İptal edildi"));
                        return;
                    }
                }
            }
        }).start();
    }

    private String cleanJsonResponse(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String readStreamAsString(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        return sb.toString();
    }

    private String readErrorBody(HttpURLConnection conn) {
        try {
            return readStreamAsString(conn.getErrorStream());
        } catch (Exception e) {
            return "";
        }
    }
}
