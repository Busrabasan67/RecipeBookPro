import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class TestGroq {
    public static void main(String[] args) {
        try {
            String apiKey = System.getenv("GROQ_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("GROQ_API_KEY environment variable must be set");
            }
            String API_URL = "https://api.groq.com/openai/v1/chat/completions";
            String CHAT_MODEL = "llama-3.1-8b-instant";

            String systemPrompt = "# ROL\n" +
            "Sen dünyanın en titiz sağlık ve gastronomi güvenlik asistanısın.\n\n" +

            "# KRİTİK KURAL — ASLA UNUTMA\n" +
            "Bu konuşmada sana gönderilen 'GÜNCEL PROFİL' dışında HİÇBİR önceki bilgiyi,\n" +
            "konuşmayı veya analizi dikkate ALMA. Her analizi sıfırdan yap.\n" +
            "Eğer bir hastalık/alerji GÜNCEL PROFİL listesinde YOKSA, o konuda ASLA uyarı verme.\n\n" +

            "# DOĞAL DİL ANALİZİ\n" +
            "Kullanıcı profili tıbbi terimler yerine günlük cümleler içerebilir.\n" +
            "Örnekler:\n" +
            "  'Limon yiyince kaşınıyorum' → Limon alerjisi → Tarifte limon, limon suyu, limon kabuğu ara.\n" +
            "  'Süt içince midem bulanıyor' → Laktoz intoleransı → Tarifte süt, peynir, tereyağı, krema ara.\n" +
            "  'I get itchy when I eat shrimp' → Shrimp allergy → Look for shrimp, prawns, shellfish.\n" +
            "  'My stomach hurts after drinking milk' → Lactose intolerance → Look for milk, cheese, butter.\n" +
            "Cümleyi analiz et, asıl alerjeni/hastalığı tespit et, sonra tarifte tara.\n\n" +

            "# SEMANTİK EŞLEŞTİRME (HER İKİ DİLDE)\n" +
            "Birebir kelime eşleşmesi YASAK. Malzemeler Türkçe veya İngilizce olabilir.\n" +
            "  Şeker/Diabetes → bal/honey, pekmez/molasses, şurup/syrup, reçel/jam, şeker/sugar\n" +
            "  Gluten/Celiac → un/flour, buğday/wheat, irmik/semolina, makarna/pasta, ekmek/bread\n" +
            "  Laktoz/Dairy → süt/milk, peynir/cheese, tereyağı/butter, krema/cream, yoğurt/yogurt\n" +
            "  Kalp/Cardiovascular → tereyağı/butter, krema/cream, sucuk/sausage, sosis, pastırma/bacon\n" +
            "  Tansiyon/Hypertension → tuz/salt, soya sosu/soy sauce\n" +
            "Kullanıcı MANUEL girdi eklemişse, o maddenin TÜM türevlerini de tara.\n\n" +

            "# ÇOK ÖNEMLİ\n" +
            "Sadece GÜNCEL PROFİL'deki koşullarla İLGİLİ malzemeleri riskli say.\n" +
            "Profilde OLMAYAN koşullar için ASLA uyarı verme.\n\n" +

            "# ANALİZ AKIŞI\n" +
            "1. GÜNCEL PROFİL'i oku ve her maddeyi anla.\n" +
            "2. Tarifte her malzemeyi kontrol et.\n" +
            "3. Sadece profildeki koşullarla gerçek risk varsa 'uygun_mu: false' döndür.\n" +
            "4. Risk yoksa 'uygun_mu: true' döndür.\n\n" +

            "# ÇIKTI FORMATI — SADECE JSON\n" +
            "JSON anahtarları HER ZAMAN Türkçe olmalı:\n" +
            "{\n" +
            "  \"uygun_mu\": true/false,\n" +
            "  \"tespit_edilen_riskli_malzemeler\": [\"malzeme1\"],\n" +
            "  \"uyari_mesaji\": \"A friendly and clear explanation in ENGLISH.\",\n" +
            "  \"tavsiye\": \"This is not medical advice. Always consult your doctor.\"\n" +
            "}\n\n" +
            "KURAL: JSON ANAHTARLARI HER ZAMAN TÜRKÇE. İngilizce key KULLANMA.\n" +
            "KURAL: TÜM metin değerleri English dilinde olmalı. ALL values must be in English only.\n" +
            "KURAL: 'uygun_mu' false ise 'tespit_edilen_riskli_malzemeler' BOŞ OLAMAZ.\n" +
            "KURAL: 'uygun_mu' true ise 'tespit_edilen_riskli_malzemeler' boş liste olmalı.\n" +
            "KURAL: JSON dışında hiçbir şey yazma.";

            String userPrompt = "# GÜNCEL PROFİL\n" +
            "Hastalıklar ve Alerjiler (SADECE bunlara bak):\n" +
            "  - Kalp ve Damar Hastalığı (Cardiovascular)\n" +
            "\n# TARİF\n" +
            "Tarif Adı: \"Salçalı Bazlama Tost\"\n" +
            "Malzemeler:\n" +
            "  - 1 number Bazlama\n" +
            "  - 1 table spoon tomato paste\n" +
            "  - 8 slice sausage\n" +
            "  - 4 slice cheddar\n" +
            "\nAnaliz et. Sadece profilde yazanlara göre değerlendir.";

            String payload = "{" +
                "\"model\": \"" + CHAT_MODEL + "\"," +
                "\"messages\": [" +
                    "{\"role\": \"system\", \"content\": \"" + systemPrompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}," +
                    "{\"role\": \"user\", \"content\": \"" + userPrompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}" +
                "]," +
                "\"response_format\": {\"type\": \"json_object\"}," +
                "\"temperature\": 0.0" +
            "}";

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            System.out.println("HTTP " + responseCode);
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) System.out.println(line);
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
