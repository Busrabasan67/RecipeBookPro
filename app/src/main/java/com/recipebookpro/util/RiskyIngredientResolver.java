package com.recipebookpro.util;

import android.text.TextUtils;

import com.recipebookpro.domain.model.Recipe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fills missing risky-ingredient lists when the API only returns prose in {@code kullanici_mesaji}.
 */
public final class RiskyIngredientResolver {

    private static final String[][] CANONICAL_GROUPS = {
            {"sugar", "şeker", "seker", "toz şeker", "toz seker", "granulated sugar", "powdered sugar",
                    "pudra şekeri", "pudra sekeri", "pudra", "şurup", "syrup", "bal", "honey"},
            {"milk", "süt", "sut", "laktoz", "lactose", "dairy", "krema", "cream", "peynir", "cheese"},
            {"flour", "un", "buğday", "bugday", "wheat", "gluten", "irmik", "semolina"},
            {"salt", "tuz", "soya sosu", "soy sauce"},
            {"egg", "yumurta", "yumurta"},
            {"butter", "tereyağı", "tereyagi", "margarin", "margarine"},
    };

    private RiskyIngredientResolver() {
    }

    public static List<String> resolveFromRecipe(Recipe recipe, List<String> healthConditions,
                                                 List<String> customConditions,
                                                 Map<String, List<String>> healthTriggers,
                                                 String rationale, String uiLang) {
        Set<String> labels = new LinkedHashSet<>();
        if (recipe == null || recipe.getIngredients() == null) {
            return new ArrayList<>();
        }

        List<String> allConditions = new ArrayList<>();
        if (healthConditions != null) {
            allConditions.addAll(healthConditions);
        }
        if (customConditions != null) {
            allConditions.addAll(customConditions);
        }

        List<String> triggers = new ArrayList<>();
        if (healthTriggers != null) {
            for (List<String> list : healthTriggers.values()) {
                if (list != null) {
                    triggers.addAll(list);
                }
            }
        }
        addConditionTriggers(allConditions, triggers, uiLang);

        for (Recipe.Ingredient ingredient : recipe.getIngredients()) {
            String combined = ingredientText(ingredient);
            if (combined.isEmpty()) {
                continue;
            }
            boolean matched = false;
            for (String trigger : triggers) {
                if (RiskyIngredientMatcher.termsOverlap(trigger, combined)) {
                    matched = true;
                    break;
                }
            }
            // Rationale-based CANONICAL_GROUPS matching removed:
            // It was too aggressive — it matched words in AI response text
            // (e.g. "süt" appearing in an explanation) with ingredients,
            // even when the user's profile had no related condition.
            if (matched) {
                String label = pickLabel(ingredient, uiLang);
                if (!TextUtils.isEmpty(label)) {
                    labels.add(label);
                }
            }
        }
        return new ArrayList<>(labels);
    }

    private static void addConditionTriggers(List<String> conditions, List<String> triggers, String uiLang) {
        boolean turkish = uiLang != null && uiLang.toLowerCase(Locale.ROOT).startsWith("tr");
        for (String condition : conditions) {
            if (condition == null) {
                continue;
            }
            String c = condition.toLowerCase(Locale.ROOT);
            if (c.contains("diabet") || c.contains("şeker") || c.contains("sugar")) {
                addWords(triggers, turkish
                        ? new String[]{"şeker", "toz şeker", "pudra şekeri", "bal", "şurup"}
                        : new String[]{"sugar", "granulated sugar", "powdered sugar", "honey", "syrup"});
            }
            if (c.contains("celiac") || c.contains("çölyak") || c.contains("gluten") || c.contains("glüten")) {
                addWords(triggers, turkish
                        ? new String[]{"un", "buğday", "irmik"}
                        : new String[]{"flour", "wheat", "gluten"});
            }
            if (c.contains("lactose") || c.contains("laktoz") || c.contains("dairy") || c.contains("süt")) {
                addWords(triggers, turkish
                        ? new String[]{"süt", "laktoz", "krema", "peynir"}
                        : new String[]{"milk", "lactose", "cream", "cheese", "dairy"});
            }
            if (c.contains("hypertension") || c.contains("tansiyon")) {
                addWords(triggers, turkish ? new String[]{"tuz", "soya sosu"} : new String[]{"salt", "soy sauce"});
            }
            if (c.contains("cardiovascular") || c.contains("kalp") || c.contains("kolesterol") || c.contains("cholesterol")) {
                addWords(triggers, turkish ? new String[]{"tereyağı", "krema", "sucuk", "sosis", "pastırma", "margarin"} : new String[]{"butter", "cream", "sausage", "bacon", "margarine"});
            }
        }
    }

    private static void addWords(List<String> triggers, String[] words) {
        for (String w : words) {
            if (!triggers.contains(w)) {
                triggers.add(w);
            }
        }
    }

    private static String ingredientText(Recipe.Ingredient ingredient) {
        return (ingredient.getName() + " "
                + ingredient.getDisplayName() + " "
                + ingredient.getDisplayText())
                .toLowerCase(Locale.ROOT);
    }

    private static String pickLabel(Recipe.Ingredient ingredient, String uiLang) {
        boolean turkish = uiLang != null && uiLang.toLowerCase(Locale.ROOT).startsWith("tr");
        String display = ingredient.getDisplayName();
        String name = ingredient.getName();
        if (turkish) {
            if (!TextUtils.isEmpty(name) && looksTurkish(name)) {
                return name.trim();
            }
            if (!TextUtils.isEmpty(display)) {
                return display.trim();
            }
            return name != null ? name.trim() : "";
        }
        if (!TextUtils.isEmpty(display)) {
            return display.trim();
        }
        return name != null ? name.trim() : "";
    }

    private static boolean looksTurkish(String text) {
        return text.matches(".*[ğüşıöçĞÜŞİÖÇ].*");
    }

    private static boolean textMentionsAny(String text, String[] words) {
        for (String word : words) {
            if (RiskyIngredientMatcher.termsOverlap(word, text)) {
                return true;
            }
        }
        return false;
    }
}
