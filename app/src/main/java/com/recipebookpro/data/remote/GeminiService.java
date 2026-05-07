package com.recipebookpro.data.remote;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import com.recipebookpro.BuildConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeminiService {

    // https://aistudio.google.com/ adresinden ücretsiz alabilirsiniz. / You can get it for free at https://aistudio.google.com/
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + API_KEY;
    // 503 hatası durumunda gemini 2.5 kullanarak api isteği gönderebiliriz
    // private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public interface GeminiCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public static void analyzeNutrition(String ingredientsText, GeminiCallback callback) {
        if ("YOUR_GEMINI_API_KEY".equals(API_KEY)) {
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (Exception ignored) {}
                callback.onSuccess("API Anahtarı eksik! Örnek Değerler:\nKalori: 450 kcal\nProtein: 18g\nKarbonhidrat: 42g\nYağ: 14g");
            }).start();
            return;
        }

        String prompt = "Sen uzman bir diyetisyensin. " +
                "Aşağıdaki malzemelerin birleşimiyle oluşan yemeğin tek porsiyonluk DİYET ve BESİN DEĞERLERİNİ tahmin et. " +
                "Format tam olarak şöyle olmalı:\nKalori: XXX kcal\nProtein: XX g\nKarbonhidrat: XX g\nYağ: XX g\n\nBaşka hiçbir yorum yazma. " +
                "Malzemeler:\n" + ingredientsText;

        try {
            JSONObject payload = createPayload(prompt);
            performRequestWithRetry(payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public static void translate(String text, String sourceLang, String targetLang, GeminiCallback callback) {
        if ("YOUR_GEMINI_API_KEY".equals(API_KEY)) {
            callback.onError("API Key missing");
            return;
        }

        String prompt = String.format("Translate the following recipe content from %s to %s. " +
                "Maintain markers like [T], [D], [I], [S] exactly. " +
                "Only translate text between them. Text:\n%s", sourceLang, targetLang, text);

        try {
            JSONObject payload = createPayload(prompt);
            performRequestWithRetry(payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    private static JSONObject createPayload(String prompt) throws Exception {
        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        payload.put("contents", contents);
        return payload;
    }

    private static void performRequestWithRetry(JSONObject payload, GeminiCallback callback) {
        new Thread(() -> {
            int maxRetries = 3;
            long retryDelayMs = 1500;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = payload.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String text = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                                
                        callback.onSuccess(text.trim());
                        return; // Başarılı oldu, thread'den çık
                    } else if (responseCode == 503 || responseCode == 500 || responseCode == 502 || responseCode == 429) {
                        if (attempt == maxRetries) {
                            callback.onError("Sunucu Hatası: " + responseCode + " (Tüm denemeler başarısız)");
                            return; // Tüm denemeler tükendi
                        }
                        Log.w("GeminiService", "API meşgul (HTTP " + responseCode + "). Yeniden deneniyor... Deneme: " + attempt);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // Exponential backoff (bekleme süresini katla)
                        continue; // Döngünün başına dön ve tekrar dene
                    } else {
                        callback.onError("Sunucu Hatası: " + responseCode);
                        return; // Diğer hatalarda (400, 401, vb.) hemen dur
                    }
                } catch (Exception e) {
                    if (attempt == maxRetries) {
                        Log.e("GeminiService", "API Error", e);
                        callback.onError(e.getMessage());
                        return;
                    }
                    try { 
                        Thread.sleep(retryDelayMs); 
                        retryDelayMs *= 2; 
                    } catch (InterruptedException ie) { 
                        Thread.currentThread().interrupt(); 
                        callback.onError("Thread kesintiye uğradı");
                        return;
                    }
                }
            }
        }).start();
    }
}
