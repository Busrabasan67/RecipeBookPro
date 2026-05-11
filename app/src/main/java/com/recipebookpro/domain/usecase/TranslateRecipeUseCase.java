package com.recipebookpro.domain.usecase;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.model.Step;
import com.recipebookpro.domain.service.TranslationService;

import java.util.ArrayList;
import java.util.List;

public class TranslateRecipeUseCase {

    private static final String TAG = "TranslateUseCase";
    private final TranslationService translationService;

    public TranslateRecipeUseCase(TranslationService translationService) {
        this.translationService = translationService;
    }

    public void execute(Recipe recipe, String targetLang, TranslationService.TranslationCallback callback) {
        if (recipe == null) {
            callback.onFailure(new IllegalArgumentException("Recipe is null"));
            return;
        }

        // 1. Build a robust text for detection
        StringBuilder sbDetection = new StringBuilder();
        sbDetection.append(recipe.getTitle()).append(" ").append(recipe.getDescription()).append(" ");
        if (recipe.getIngredients() != null) {
            for (Recipe.Ingredient ing : recipe.getIngredients()) {
                sbDetection.append(ing.getName()).append(" ").append(ing.getUnit()).append(" ");
            }
        }
        String text = sbDetection.toString().toLowerCase();
        
        callback.onDownloadProgress("Analyzing language...");

        LanguageIdentifier identifier = LanguageIdentification.getClient();
        identifier.identifyLanguage(text)
                .addOnSuccessListener(sourceLang -> {
                    String finalSource = sourceLang;
                    
                    // 2. Enhanced Keyword Balance Analysis
                    String enKeywords = "\\b(cup|water|milk|egg|piece|tablespoon|teaspoon|sugar|salt|oil|flour|butter|garlic|onion|pepper|salt|chicken|meat|beef|cook|fry|boil|bake)\\b";
                    String trKeywords = "\\b(su|süt|yumurta|adet|kaşık|şeker|tuz|yağ|un|tereyağı|sarımsak|soğan|biber|tavuk|et|pişir|kızart|kaynat|fırın|bardak|fincan)\\b";
                    
                    int enCount = countMatches(text, enKeywords);
                    int trCount = countMatches(text, trKeywords);
                    
                    if (sourceLang.equals("und") || Math.abs(enCount - trCount) > 1) {
                        if (enCount > trCount) finalSource = "en";
                        else if (trCount > enCount) finalSource = "tr";
                    }
                    
                    recipe.setOriginalLanguage(finalSource);
                    Log.d(TAG, "Detection result: " + finalSource + " (EN:" + enCount + ", TR:" + trCount + ")");
                    
                    if (finalSource.equalsIgnoreCase(targetLang)) {
                        recipe.clearAllTranslations();
                        callback.onSuccess("Match found");
                    } else {
                        prepareAndThenTranslate(recipe, finalSource, targetLang, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Detection failed", e);
                    callback.onFailure(e);
                });
    }

    private int countMatches(String text, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    private void prepareAndThenTranslate(Recipe recipe, String source, String target, TranslationService.TranslationCallback callback) {
        callback.onDownloadProgress("Downloading translation models...");
        
        translationService.prepareModel(source, target)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Models ready. Starting field translation...");
                    startFieldTranslation(recipe, source, target, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Model preparation failed", e);
                    callback.onFailure(e);
                });
    }

    private void startFieldTranslation(Recipe recipe, String source, String target, TranslationService.TranslationCallback callback) {
        callback.onDownloadProgress("Translating recipe details...");

        List<Task<String>> tasks = new ArrayList<>();

        // Order is critical for applyResults mapping
        tasks.add(translationService.translateSingleField(recipe.getTitle(), source, target));
        tasks.add(translationService.translateSingleField(recipe.getDescription(), source, target));

        if (recipe.getIngredients() != null) {
            for (Recipe.Ingredient ing : recipe.getIngredients()) {
                tasks.add(translationService.translateSingleField(ing.getName(), source, target));
                if (ing.getUnit() != null && !ing.getUnit().isEmpty()) {
                    tasks.add(translationService.translateSingleField(ing.getUnit(), source, target));
                }
            }
        }

        if (recipe.getStepList() != null) {
            for (Step step : recipe.getStepList()) {
                tasks.add(translationService.translateSingleField(step.getDescription(), source, target));
            }
        }

        if (recipe.getAllergens() != null) {
            for (String allergen : recipe.getAllergens()) {
                tasks.add(translationService.translateSingleField(allergen, source, target));
            }
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(allTasks -> {
            if (allTasks.isSuccessful()) {
                applyResults(recipe, tasks, target);
                callback.onSuccess("Success");
            } else {
                callback.onFailure(new Exception("Some fields failed to translate"));
            }
        });
    }

    private void applyResults(Recipe recipe, List<Task<String>> tasks, String targetLang) {
        int index = 0;
        recipe.setTranslatedTitle(getTaskResult(tasks.get(index++), recipe.getTitle(), targetLang));
        recipe.setTranslatedDescription(getTaskResult(tasks.get(index++), recipe.getDescription(), targetLang));

        if (recipe.getIngredients() != null) {
            for (Recipe.Ingredient ing : recipe.getIngredients()) {
                ing.setTranslatedName(getTaskResult(tasks.get(index++), ing.getName(), targetLang));
                if (ing.getUnit() != null && !ing.getUnit().isEmpty()) {
                    ing.setTranslatedUnit(getTaskResult(tasks.get(index++), ing.getUnit(), targetLang));
                } else {
                    ing.setTranslatedUnit("");
                }
            }
        }

        if (recipe.getStepList() != null) {
            for (Step step : recipe.getStepList()) {
                step.setTranslatedDescription(getTaskResult(tasks.get(index++), step.getDescription(), targetLang));
            }
        }

        // Rebuild steps text
        StringBuilder sb = new StringBuilder();
        if (recipe.getStepList() != null) {
            for (Step s : recipe.getStepList()) {
                sb.append(s.getDisplayDescription()).append("\n");
            }
        }
        recipe.setTranslatedInstructions(sb.toString().trim());

        List<String> translatedAllergens = new ArrayList<>();
        if (recipe.getAllergens() != null) {
            for (String allergen : recipe.getAllergens()) {
                translatedAllergens.add(getTaskResult(tasks.get(index++), allergen, targetLang));
            }
        }
        recipe.setTranslatedAllergens(translatedAllergens);
    }

    private String getTaskResult(Task<String> task, String originalText, String targetLang) {
        if (task.isSuccessful() && task.getResult() != null) {
            String result = task.getResult().trim();
            // Manual Patch: If AI returns the same short word, use a small internal dictionary
            if (result.equalsIgnoreCase(originalText.trim()) && originalText.length() < 5) {
                return applyManualPatch(originalText.trim().toLowerCase(), targetLang);
            }
            return result;
        }
        return originalText; // Fallback to original if failed
    }

    private String applyManualPatch(String text, String targetLang) {
        String input = text.trim().toLowerCase();
        if (targetLang.equals("en")) {
            if (input.equals("un")) return "flour";
            if (input.equals("su")) return "water";
            if (input.equals("et")) return "meat";
            if (input.equals("tuz")) return "salt";
            if (input.equals("süt")) return "milk";
            if (input.equals("yağ")) return "oil";
            if (input.equals("karis") || input.equals("karış")) return "span";
            if (input.equals("adet")) return "piece";
            if (input.equals("bardak")) return "glass";
            if (input.equals("kaşık")) return "spoon";
            if (input.equals("mısır") || input.equals("misir")) return "corn";
            if (input.equals("maydanoz")) return "parsley";
            if (input.equals("dereotu")) return "dill";
            if (input.equals("un")) return "flour";
        } else if (targetLang.equals("tr")) {
            if (input.equals("flour")) return "un";
            if (input.equals("water")) return "su";
            if (input.equals("meat")) return "et";
            if (input.equals("salt")) return "tuz";
            if (input.equals("milk")) return "süt";
            if (input.equals("oil")) return "yağ";
            if (input.equals("span") || input.equals("hand span")) return "karış";
            if (input.equals("piece")) return "adet";
            if (input.equals("glass") || input.equals("cup")) return "bardak";
            if (input.equals("spoon")) return "kaşık";
            if (input.equals("corn")) return "mısır";
            if (input.equals("parsley")) return "maydanoz";
            if (input.equals("dill")) return "dereotu";
        }
        return text;
    }
}
