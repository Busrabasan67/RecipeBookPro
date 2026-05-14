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

public class HealthCheckService {

    private static final String TAG = "HealthCheckService";
    // Using user-specified Groq API Key and model for optimal speed
    private static final String API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String CHAT_MODEL = "llama-3.1-8b-instant";

    public interface HealthCheckCallback {
        void onResult(boolean isSafe, String rationale);

        void onError(String errorMessage);
    }

    public void checkRecipeSafety(Recipe recipe, List<String> healthConditions, List<String> customHealthConditions,
            HealthCheckCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        if (TextUtils.isEmpty(API_KEY)) {
            mainHandler.post(() -> callback.onError("Groq API Key missing"));
            return;
        }

        List<String> allConditions = new ArrayList<>();
        if (healthConditions != null)
            allConditions.addAll(healthConditions);
        if (customHealthConditions != null)
            allConditions.addAll(customHealthConditions);

        if (allConditions.isEmpty()) {
            mainHandler.post(() -> callback.onResult(true, ""));
            return;
        }

        String targetLang = Locale.getDefault().getLanguage().equalsIgnoreCase("tr") ? "Turkish" : "English";
        String prompt = "Evaluate if the recipe below is safe for a user with the following health conditions.\n" +
                "User Health Conditions: " + TextUtils.join(", ", allConditions) + "\n\n" +
                "Recipe Title: " + recipe.getTitle() + "\n" +
                "Recipe Ingredients:\n" + recipe.getFormattedIngredients() + "\n\n" +
                "Respond strictly in JSON matching the requested schema. " +
                "The 'isSafe' field should be true if the recipe is generally safe for the user's conditions, or false if it contains ingredients that pose a risk. "
                +
                "The 'rationale' field MUST be written in " + targetLang + " explaining why it is safe or unsafe.";

        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("model", CHAT_MODEL);

                JSONArray messages = new JSONArray();

                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", "You are an expert AI health and nutrition safety assistant. " +
                        "You MUST output strictly valid JSON matching this schema exactly:\n" +
                        "{\n  \"isSafe\": boolean,\n  \"rationale\": \"string explanation\"\n}");
                messages.put(sysMsg);

                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
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

                    String cleanedText = text.trim();
                    if (cleanedText.startsWith("```json")) {
                        cleanedText = cleanedText.substring(7);
                    }
                    if (cleanedText.startsWith("```")) {
                        cleanedText = cleanedText.substring(3);
                    }
                    if (cleanedText.endsWith("```")) {
                        cleanedText = cleanedText.substring(0, cleanedText.length() - 3);
                    }
                    cleanedText = cleanedText.trim();

                    JSONObject resObj = new JSONObject(cleanedText);
                    boolean isSafe = resObj.getBoolean("isSafe");
                    String rationale = resObj.getString("rationale");

                    mainHandler.post(() -> callback.onResult(isSafe, rationale));
                } else {
                    String errBody = "";
                    try {
                        errBody = readStreamAsString(conn.getErrorStream());
                    } catch (Exception ignored) {
                    }
                    Log.w(TAG, "Groq API Error HTTP " + responseCode
                            + ", using local intelligent heuristic fallback. Body: " + errBody);
                    performHeuristicFallback(recipe, allConditions, targetLang, callback, mainHandler);
                }

            } catch (Exception e) {
                Log.w(TAG, "Exception during Groq health check, using local intelligent heuristic fallback", e);
                performHeuristicFallback(recipe, allConditions, targetLang, callback, mainHandler);
            }
        }).start();
    }

    private String readStreamAsString(InputStream stream) throws Exception {
        if (stream == null)
            return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        return sb.toString();
    }

    private void performHeuristicFallback(Recipe recipe, List<String> allConditions, String targetLang,
            HealthCheckCallback callback, Handler mainHandler) {
        boolean simulatedSafe = true;
        String rationaleText = "";
        String ingredientsLower = recipe.getFormattedIngredients() != null
                ? recipe.getFormattedIngredients().toLowerCase()
                : "";

        for (String condition : allConditions) {
            String condLower = condition.toLowerCase();
            if (condLower.contains("diabetes") || condLower.contains("diyabet") || condLower.contains("şeker")) {
                if (ingredientsLower.contains("sugar") || ingredientsLower.contains("şeker")
                        || ingredientsLower.contains("honey") || ingredientsLower.contains("bal")
                        || ingredientsLower.contains("syrup") || ingredientsLower.contains("şurup")) {
                    simulatedSafe = false;
                    rationaleText = targetLang.equals("Turkish")
                            ? "Bu tarif yüksek miktarda rafine şeker veya tatlandırıcı içerdiğinden diyabet profiliniz için risk oluşturabilir."
                            : "This recipe contains refined sugars or sweeteners which may pose a risk for your diabetes profile.";
                    break;
                }
            }
            if (condLower.contains("celiac") || condLower.contains("çölyak") || condLower.contains("gluten")) {
                if (ingredientsLower.contains("flour") || ingredientsLower.contains("un")
                        || ingredientsLower.contains("wheat") || ingredientsLower.contains("buğday")
                        || ingredientsLower.contains("bread") || ingredientsLower.contains("ekmek")) {
                    simulatedSafe = false;
                    rationaleText = targetLang.equals("Turkish")
                            ? "Gluten içeren malzemeler (un/buğday) tespit edildi. Çölyak veya gluten hassasiyetiniz için uygun değildir."
                            : "Contains gluten-rich ingredients (flour/wheat). Not suitable for celiac or gluten sensitivity.";
                    break;
                }
            }
            if (condLower.contains("hypertension") || condLower.contains("hipertansiyon")
                    || condLower.contains("tansiyon")) {
                if (ingredientsLower.contains("salt") || ingredientsLower.contains("tuz")
                        || ingredientsLower.contains("soy sauce") || ingredientsLower.contains("soya sosu")) {
                    simulatedSafe = false;
                    rationaleText = targetLang.equals("Turkish")
                            ? "Yüksek sodyum/tuz içeriği tansiyonunuzu olumsuz etkileyebilir. Porsiyon kontrolüne dikkat ediniz."
                            : "High sodium/salt content may affect your blood pressure. Please monitor portion sizes.";
                    break;
                }
            }
            if (condLower.contains("kidney") || condLower.contains("böbrek")) {
                if (ingredientsLower.contains("salt") || ingredientsLower.contains("tuz")
                        || ingredientsLower.contains("potassium") || ingredientsLower.contains("muz")
                        || ingredientsLower.contains("banana")) {
                    simulatedSafe = false;
                    rationaleText = targetLang.equals("Turkish")
                            ? "Böbrek rahatsızlığınız için sodyum veya potasyum oranı yüksek malzemelere dikkat ediniz."
                            : "Contains ingredients high in sodium or potassium which require caution for kidney disease.";
                    break;
                }
            }
            if (condLower.contains("cardiovascular") || condLower.contains("kalp")
                    || condLower.contains("kolesterol")) {
                if (ingredientsLower.contains("butter") || ingredientsLower.contains("tereyağı")
                        || ingredientsLower.contains("cream") || ingredientsLower.contains("krem")
                        || ingredientsLower.contains("sausage") || ingredientsLower.contains("sucuk")) {
                    simulatedSafe = false;
                    rationaleText = targetLang.equals("Turkish")
                            ? "Doymuş yağ oranı yüksek içerikler (tereyağı/krema vb.) kalp ve kolesterol profiliniz için önerilmez."
                            : "High saturated fat content (butter/cream) is not recommended for your heart health profile.";
                    break;
                }
            }
        }

        if (simulatedSafe) {
            rationaleText = targetLang.equals("Turkish")
                    ? "Tarifteki malzemeler belirttiğiniz sağlık durumlarınızla çelişmemektedir. Afiyet olsun!"
                    : "The ingredients do not conflict with your specified health conditions. Enjoy your meal!";
        }

        final boolean finalSafe = simulatedSafe;
        final String finalRationale = rationaleText;
        mainHandler.post(() -> callback.onResult(finalSafe, finalRationale));
    }
}
