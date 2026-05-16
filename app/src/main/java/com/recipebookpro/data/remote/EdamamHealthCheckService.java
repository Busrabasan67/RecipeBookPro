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
 * Edamam Nutrition Analysis API kullanarak tarif malzemelerini analiz eder.
 * Dönen healthLabels ve dietLabels'ı kullanıcı profiliyle karşılaştırarak
 * riskli malzemeleri tespit eder ve uyarı mesajı üretir.
 */
public class EdamamHealthCheckService {

    private static final String TAG = "EdamamHealthCheck";
    private static final String APP_ID  = BuildConfig.EDAMAM_APP_ID;
    private static final String APP_KEY = BuildConfig.EDAMAM_APP_KEY;
    private static final String API_URL =
            "https://api.edamam.com/api/nutrition-details?app_id=" + APP_ID + "&app_key=" + APP_KEY;

    public interface EdamamCallback {
        void onResult(boolean isSafe, String rationale, List<String> riskyIngredients,
                      List<String> healthLabels);
        void onError(String errorMessage);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Kullanıcı profil koşulu  →  Edamam healthLabel eşlemeleri
    // "label yoksa" → tarif bu özellikte değil = kullanıcı için riskli
    // ──────────────────────────────────────────────────────────────────────────
    private static final Object[][] CONDITION_TO_LABEL = {
            // { koşul anahtar kelimeleri,  beklenen Edamam label,         TR uyarı,                                EN uyarı }
            { new String[]{"celiac","çölyak","gluten","glüten"},
                    "GLUTEN_FREE",
                    "Bu tarif gluten içeriyor. Çölyak/gluten hassasiyetiniz için uygun değil.",
                    "This recipe contains gluten. Not suitable for celiac/gluten sensitivity." },

            { new String[]{"peanut","fıstık","yerfıstığı","peanut allergy"},
                    "PEANUT_FREE",
                    "Bu tarif yerfıstığı içeriyor veya içerebilir.",
                    "This recipe contains or may contain peanuts." },

            { new String[]{"tree nut","fındık","ceviz","badem","kaju","nut allergy","kuruyemiş"},
                    "TREE_NUT_FREE",
                    "Bu tarif sert kabuklu yemiş içeriyor (fındık, ceviz, badem vb.).",
                    "This recipe contains tree nuts (hazelnuts, walnuts, almonds, etc.)." },

            { new String[]{"dairy","süt","milk","laktoz","lactose","süt alerjisi","dairy allergy"},
                    "DAIRY_FREE",
                    "Bu tarif süt ürünleri içeriyor. Süt/laktoz hassasiyetiniz için uygun değil.",
                    "This recipe contains dairy products. Not suitable for lactose intolerance or dairy allergy." },

            { new String[]{"egg","yumurta","egg allergy"},
                    "EGG_FREE",
                    "Bu tarif yumurta içeriyor.",
                    "This recipe contains eggs." },

            { new String[]{"soy","soya","soy allergy"},
                    "SOY_FREE",
                    "Bu tarif soya içeriyor.",
                    "This recipe contains soy." },

            { new String[]{"fish","balık","fish allergy"},
                    "FISH_FREE",
                    "Bu tarif balık içeriyor.",
                    "This recipe contains fish." },

            { new String[]{"shellfish","kabuklu deniz","shrimp","karides","seafood"},
                    "SHELLFISH_FREE",
                    "Bu tarif kabuklu deniz ürünleri içeriyor.",
                    "This recipe contains shellfish/seafood." },

            { new String[]{"sesame","susam","sesame allergy"},
                    "SESAME_FREE",
                    "Bu tarif susam içeriyor.",
                    "This recipe contains sesame." },

            { new String[]{"wheat","buğday","wheat allergy"},
                    "WHEAT_FREE",
                    "Bu tarif buğday içeriyor.",
                    "This recipe contains wheat." },

            { new String[]{"hypertension","hipertansiyon","tansiyon","yüksek tansiyon","high blood pressure"},
                    "LOW_SODIUM_DIET",
                    "Bu tarif yüksek sodyum (tuz) içeriyor. Tansiyon profiliniz için dikkatli tüketin.",
                    "This recipe is high in sodium (salt). Please consume cautiously for hypertension." },

            { new String[]{"diabetes","diyabet","şeker hastalığı","tip 2","type 2 diabetes"},
                    "DIABETIC",
                    "Bu tarif rafine karbonhidrat/şeker içeriyor. Diyabet profiliniz için dikkatli tüketin.",
                    "This recipe contains refined carbs/sugars. Please consume cautiously for diabetes." },

            { new String[]{"kidney","böbrek","renal"},
                    "RENAL_DIET",
                    "Bu tarifteki sodyum veya potasyum içeriği böbrek rahatsızlığınız için dikkat gerektirir.",
                    "This recipe's sodium or potassium content requires caution for renal (kidney) disease." },

            { new String[]{"cardiovascular","kalp","heart","kolesterol","cholesterol"},
                    "LOW_FAT_ABS",
                    "Bu tarif yüksek doymuş yağ içeriyor. Kalp/kolesterol profiliniz için dikkatli tüketin.",
                    "This recipe is high in saturated fats. Please consume cautiously for cardiovascular health." },

            { new String[]{"vegan","vejgan"},
                    "VEGAN",
                    "Bu tarif hayvansal ürün içeriyor. Vegan diyet profilinizle uyuşmuyor.",
                    "This recipe contains animal products. Not compatible with a vegan diet." },

            { new String[]{"vegetarian","vejeteryan"},
                    "VEGETARIAN",
                    "Bu tarif et ürünleri içeriyor. Vejeteryan profilinizle uyuşmuyor.",
                    "This recipe contains meat. Not compatible with a vegetarian diet." },

            { new String[]{"keto","ketojenik"},
                    "KETO_FRIENDLY",
                    "Bu tarif keto diyetiyle uyumlu değil (yüksek karbonhidrat içeriyor).",
                    "This recipe is not keto-friendly (high carbohydrate content)." },

            { new String[]{"paleo"},
                    "PALEO",
                    "Bu tarif paleo diyetiyle uyumlu değil.",
                    "This recipe is not compatible with a paleo diet." },

            { new String[]{"alcohol","alkol"},
                    "ALCOHOL_FREE",
                    "Bu tarif alkol içeriyor.",
                    "This recipe contains alcohol." },
    };

    // ──────────────────────────────────────────────────────────────────────────
    // Ana analiz metodu
    // ──────────────────────────────────────────────────────────────────────────
    public void analyze(Recipe recipe,
                        List<String> healthConditions,
                        List<String> customConditions,
                        List<String> allergens,
                        Map<String, List<String>> healthTriggers,
                        String uiLangCode,
                        EdamamCallback callback) {

        final Handler mainHandler = new Handler(Looper.getMainLooper());

        if (TextUtils.isEmpty(APP_ID) || TextUtils.isEmpty(APP_KEY)) {
            mainHandler.post(() -> callback.onError("Edamam API anahtarı eksik"));
            return;
        }

        List<String> allConditions = buildAllConditions(healthConditions, customConditions, allergens);
        if (allConditions.isEmpty()) {
            mainHandler.post(() -> callback.onResult(true, "", new ArrayList<>(), new ArrayList<>()));
            return;
        }

        List<String> ingredientLines = buildIngredientLines(recipe);
        if (ingredientLines.isEmpty()) {
            mainHandler.post(() -> callback.onResult(true, "", new ArrayList<>(), new ArrayList<>()));
            return;
        }

        boolean isTurkish = uiLangCode != null && uiLangCode.toLowerCase(Locale.ROOT).startsWith("tr");

        new Thread(() -> {
            try {
                // Edamam isteği oluştur
                JSONObject payload = new JSONObject();
                payload.put("title", recipe.getTitle());
                JSONArray ingrArr = new JSONArray();
                for (String line : ingredientLines) ingrArr.put(line);
                payload.put("ingr", ingrArr);

                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    String body = readStream(conn.getInputStream());
                    JSONObject resp = new JSONObject(body);

                    // healthLabels listesini oku
                    List<String> edamamLabels = new ArrayList<>();
                    JSONArray labelsArr = resp.optJSONArray("healthLabels");
                    if (labelsArr != null) {
                        for (int i = 0; i < labelsArr.length(); i++) {
                            String lbl = labelsArr.optString(i, "").toUpperCase(Locale.ROOT);
                            if (!lbl.isEmpty()) edamamLabels.add(lbl);
                        }
                    }
                    JSONArray dietArr = resp.optJSONArray("dietLabels");
                    if (dietArr != null) {
                        for (int i = 0; i < dietArr.length(); i++) {
                            String lbl = dietArr.optString(i, "").toUpperCase(Locale.ROOT);
                            if (!lbl.isEmpty()) edamamLabels.add(lbl);
                        }
                    }

                    Log.d(TAG, "Edamam labels: " + edamamLabels);

                    // Kullanıcı profilini Edamam etiketleriyle karşılaştır
                    List<String> warnings   = new ArrayList<>();
                    List<String> riskyIngrs = new ArrayList<>();

                    for (String condition : allConditions) {
                        String condLower = condition.toLowerCase(Locale.ROOT);
                        for (Object[] row : CONDITION_TO_LABEL) {
                            String[] keywords  = (String[]) row[0];
                            String   label     = (String)   row[1];
                            String   warnTr    = (String)   row[2];
                            String   warnEn    = (String)   row[3];

                            boolean conditionMatches = false;
                            for (String kw : keywords) {
                                if (condLower.contains(kw)) { conditionMatches = true; break; }
                            }
                            if (!conditionMatches) continue;

                            // Eğer Edamam "bu tarif X-free" diyemiyorsa → riskli
                            if (!edamamLabels.contains(label)) {
                                String warning = isTurkish ? warnTr : warnEn;
                                if (!warnings.contains(warning)) warnings.add(warning);

                                // Riskli malzeme olarak etiket ile eşleşen ingredientleri bul
                                List<String> matched = findMatchingIngredients(recipe, keywords);
                                for (String m : matched) {
                                    if (!riskyIngrs.contains(m)) riskyIngrs.add(m);
                                }
                            }
                        }
                    }

                    // Groq healthTriggers ile ek kontrol (özel alerjiler için)
                    if (healthTriggers != null && !healthTriggers.isEmpty()) {
                        List<String> extraRisky = matchTriggersToIngredients(recipe, healthTriggers);
                        for (String r : extraRisky) {
                            if (!riskyIngrs.contains(r)) riskyIngrs.add(r);
                        }
                        if (!extraRisky.isEmpty() && warnings.isEmpty()) {
                            String extra = isTurkish
                                    ? "Sağlık profilinize göre bazı malzemeler risk oluşturabilir: " + TextUtils.join(", ", extraRisky)
                                    : "Some ingredients may pose a risk based on your health profile: " + TextUtils.join(", ", extraRisky);
                            warnings.add(extra);
                        }
                    }

                    boolean isSafe = warnings.isEmpty();
                    String rationale = isSafe
                            ? (isTurkish ? "Tarif sağlık profilinizle uyumlu görünüyor. Afiyet olsun!" : "The recipe appears compatible with your health profile. Enjoy!")
                            : TextUtils.join("\n", warnings);

                    final boolean finalSafe = isSafe;
                    final String finalRationale = rationale;
                    final List<String> finalRisky = new ArrayList<>(riskyIngrs);
                    final List<String> finalLabels = new ArrayList<>(edamamLabels);
                    mainHandler.post(() -> callback.onResult(finalSafe, finalRationale, finalRisky, finalLabels));

                } else if (code == 555) {
                    // Edamam malzemeleri parse edemedi – Groq fallback'e geç
                    Log.w(TAG, "Edamam 555: ingredient parse failed, triggering fallback");
                    mainHandler.post(() -> callback.onError("Edamam malzeme listesini ayrıştıramadı (555)"));
                } else {
                    String errBody = "";
                    try { errBody = readStream(conn.getErrorStream()); } catch (Exception ignored) {}
                    Log.w(TAG, "Edamam HTTP " + code + ": " + errBody);
                    mainHandler.post(() -> callback.onError("Edamam API hatası: " + code));
                }

            } catch (Exception e) {
                Log.w(TAG, "Edamam exception", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Yardımcı metodlar
    // ──────────────────────────────────────────────────────────────────────────

    private List<String> buildAllConditions(List<String> health, List<String> custom, List<String> allergens) {
        List<String> all = new ArrayList<>();
        if (health != null)    all.addAll(health);
        if (custom != null)    all.addAll(custom);
        if (allergens != null) all.addAll(allergens);
        return all;
    }

    private List<String> buildIngredientLines(Recipe recipe) {
        List<String> lines = new ArrayList<>();
        if (recipe.getIngredients() == null) return lines;
        for (Recipe.Ingredient ing : recipe.getIngredients()) {
            String line = ing.getDisplayText().trim();
            if (!line.isEmpty()) lines.add(line);
        }
        return lines;
    }

    /** Tarif malzemeleri içinde anahtar kelimelerden herhangi biriyle eşleşenleri döndür */
    private List<String> findMatchingIngredients(Recipe recipe, String[] keywords) {
        List<String> result = new ArrayList<>();
        if (recipe.getIngredients() == null) return result;
        for (Recipe.Ingredient ing : recipe.getIngredients()) {
            String text = (ing.getName() + " " + ing.getDisplayName()).toLowerCase(Locale.ROOT);
            for (String kw : keywords) {
                if (text.contains(kw.toLowerCase(Locale.ROOT))) {
                    String label = ing.getDisplayName().isEmpty() ? ing.getName() : ing.getDisplayName();
                    if (!label.isEmpty() && !result.contains(label)) result.add(label);
                    break;
                }
            }
        }
        return result;
    }

    /** healthTriggers (Groq'un ürettiği tetikleyiciler) ile recipe malzemelerini eşleştir */
    private List<String> matchTriggersToIngredients(Recipe recipe,
                                                     Map<String, List<String>> healthTriggers) {
        List<String> risky = new ArrayList<>();
        if (recipe.getIngredients() == null) return risky;
        for (Recipe.Ingredient ing : recipe.getIngredients()) {
            String ingText = (ing.getName() + " " + ing.getDisplayName()).toLowerCase(Locale.ROOT);
            outer:
            for (List<String> triggers : healthTriggers.values()) {
                if (triggers == null) continue;
                for (String trigger : triggers) {
                    if (ingText.contains(trigger.toLowerCase(Locale.ROOT))) {
                        String label = ing.getDisplayName().isEmpty() ? ing.getName() : ing.getDisplayName();
                        if (!label.isEmpty() && !risky.contains(label)) risky.add(label);
                        break outer;
                    }
                }
            }
        }
        return risky;
    }

    private String readStream(java.io.InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }
}
