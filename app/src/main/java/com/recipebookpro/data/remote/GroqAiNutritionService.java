package com.recipebookpro.data.remote;

import android.util.Log;

import com.recipebookpro.BuildConfig;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.service.AiNutritionService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GroqAiNutritionService implements AiNutritionService {

    private static final String TAG = "GroqAiNutrition";

    private static final String API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String CHAT_MODEL = "llama-3.1-8b-instant";

    @Override
    public void analyzeMacrosFromIngredients(String ingredientsText, ResultCallback callback) {
        if ("YOUR_GROQ_API_KEY".equals(API_KEY)) {
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    callback.onError("Interrupted");
                    return;
                }
                callback.onSuccess("API Anahtarı eksik! Örnek Değerler:\nKalori: 450 kcal\nProtein: 18g\nKarbonhidrat: 42g\nYağ: 14g\n(Lütfen local.properties'e GROQ_API_KEY ekleyin)");
            }).start();
            return;
        }

        String prompt = "Sen uzman bir diyetisyensin. " +
                "Aşağıdaki malzemelerin birleşimiyle oluşan yemeğin tek porsiyonluk DİYET ve BESİN DEĞERLERİNİ tahmin et. " +
                "Format tam olarak şöyle olmalı:\nKalori: XXX kcal\nProtein: XX g\nKarbonhidrat: XX g\nYağ: XX g\n\nBaşka hiçbir yorum yazma. Sadece 4 satır besin değeri döndür. " +
                "Malzemeler:\n" + ingredientsText;

        try {
            JSONObject payload = createPayload(prompt);
            performRequestWithRetry(payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    @Override
    public void estimateTotalCaloriesForRecipe(Recipe recipe, ResultCallback callback) {
        if ("YOUR_GROQ_API_KEY".equals(API_KEY)) {
            callback.onError("API Key missing");
            return;
        }

        StringBuilder ingredients = new StringBuilder();
        if (recipe.getIngredients() != null) {
            for (Recipe.Ingredient ing : recipe.getIngredients()) {
                ingredients.append(ing.getAmount()).append(" ").append(ing.getUnit()).append(" ").append(ing.getName()).append("\n");
            }
        }

        String prompt = "Aşağıdaki malzemelerin toplamıyla oluşan yemeğin tamamı için toplam kalorisini tahmin et. " +
                "Sadece ve sadece bir tam sayı döndür. (Örn: 450). Hiçbir metin, harf, yorum veya birim (kcal vb.) yazma. " +
                "Eğer boşsa veya tahmine uygun değilse 0 döndür. Malzemeler:\n" + ingredients;

        try {
            JSONObject payload = createPayload(prompt);
            performRequestWithRetry(payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    private static JSONObject createPayload(String prompt) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("model", CHAT_MODEL);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);

        payload.put("messages", messages);
        payload.put("temperature", 0.2);

        return payload;
    }

    private static String readStreamAsString(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        return sb.toString();
    }

    private static String parseGroqErrorMessage(String body) {
        if (body == null || body.isEmpty()) return "";
        try {
            JSONObject root = new JSONObject(body);
            if (root.has("error")) {
                Object err = root.get("error");
                if (err instanceof JSONObject) {
                    String msg = ((JSONObject) err).optString("message", "");
                    if (!msg.isEmpty()) return msg;
                } else if (err instanceof String) {
                    return (String) err;
                }
            }
        } catch (Exception ignored) { }
        return body.length() > 200 ? body.substring(0, 200) + "…" : body;
    }

    private static void performRequestWithRetry(JSONObject payload, ResultCallback callback) {
        new Thread(() -> {
            int maxRetries = 3;
            long retryDelayMs = 1500;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                    conn.setDoOutput(true);

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

                        callback.onSuccess(text.trim());
                        return;
                    } else if (responseCode == 429 || responseCode >= 500) {
                        if (attempt == maxRetries) {
                            String errBody = "";
                            try {
                                errBody = readStreamAsString(conn.getErrorStream());
                            } catch (Exception ignored) {}
                            Log.e(TAG, "HTTP " + responseCode + " after retries; body=" + errBody);
                            String detail = parseGroqErrorMessage(errBody);
                            if (!detail.isEmpty()) {
                                callback.onError(detail + " (" + responseCode + ")");
                            } else {
                                callback.onError("Sunucu Hatası: " + responseCode + " (Tüm denemeler başarısız)");
                            }
                            return;
                        }
                        Log.w(TAG, "API meşgul (HTTP " + responseCode + "). Yeniden deneniyor... Deneme: " + attempt);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2;
                        continue;
                    } else {
                        String errBody = "";
                        try {
                            errBody = readStreamAsString(conn.getErrorStream());
                        } catch (Exception readEx) {
                            Log.e(TAG, "Error stream read failed", readEx);
                        }
                        Log.e(TAG, "HTTP " + responseCode + " — " + errBody);

                        String detail = parseGroqErrorMessage(errBody);
                        if (!detail.isEmpty()) {
                            callback.onError(detail + " (" + responseCode + ")");
                        } else {
                            callback.onError("Sunucu Hatası: " + responseCode);
                        }
                        return;
                    }
                } catch (Exception e) {
                    if (attempt == maxRetries) {
                        Log.e(TAG, "API Error", e);
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
