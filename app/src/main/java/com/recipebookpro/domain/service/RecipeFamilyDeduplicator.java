package com.recipebookpro.domain.service;

import com.recipebookpro.domain.model.Recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecipeFamilyDeduplicator {

    private RecipeFamilyDeduplicator() {
    }

    public static List<Recipe> keepLatestRecipePerFamily(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Recipe> recipesById = new HashMap<>();
        for (Recipe recipe : recipes) {
            if (recipe == null) {
                continue;
            }
            String id = normalize(recipe.getId());
            if (!id.isEmpty()) {
                recipesById.put(id, recipe);
            }
        }

        Map<String, Recipe> latestByFamily = new LinkedHashMap<>();
        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            if (recipe == null) {
                continue;
            }

            String familyKey = resolveFamilyKey(recipe, recipesById, i);
            Recipe current = latestByFamily.get(familyKey);
            if (current == null || recipe.getCreatedAt() >= current.getCreatedAt()) {
                latestByFamily.put(familyKey, recipe);
            }
        }

        return new ArrayList<>(latestByFamily.values());
    }

    private static String resolveFamilyKey(Recipe recipe, Map<String, Recipe> recipesById, int fallbackIndex) {
        String sourceRecipeId = normalize(recipe.getSourceRecipeId());
        String id = normalize(recipe.getId());
        String currentKey = !sourceRecipeId.isEmpty() ? sourceRecipeId : id;
        if (currentKey.isEmpty()) {
            return "__recipe_without_identity_" + fallbackIndex;
        }

        Set<String> visitedIds = new HashSet<>();
        while (!currentKey.isEmpty() && visitedIds.add(currentKey)) {
            Recipe sourceRecipe = recipesById.get(currentKey);
            if (sourceRecipe == null) {
                break;
            }

            String parentSourceId = normalize(sourceRecipe.getSourceRecipeId());
            if (parentSourceId.isEmpty()) {
                break;
            }
            currentKey = parentSourceId;
        }

        return currentKey;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
