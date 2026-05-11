package com.recipebookpro.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Planner ekranındaki günlük haritadan türeyen kalori özeti (saf domain hesabı).
 */
public final class PlannerCalorieSummary {

    public enum Coverage {
        NONE,
        ALL_KNOWN,
        PARTIAL,
        ALL_UNKNOWN
    }

    private final int totalKnownCalories;
    private final int unknownRecipeCount;
    private final int recipeCount;

    private PlannerCalorieSummary(int totalKnownCalories, int unknownRecipeCount, int recipeCount) {
        this.totalKnownCalories = totalKnownCalories;
        this.unknownRecipeCount = unknownRecipeCount;
        this.recipeCount = recipeCount;
    }

    public static PlannerCalorieSummary from(Map<String, List<Recipe>> dayRecipes) {
        int totalKnown = 0;
        int unknown = 0;
        int count = 0;
        if (dayRecipes != null) {
            for (List<Recipe> list : dayRecipes.values()) {
                if (list == null) continue;
                for (Recipe r : list) {
                    if (r == null) continue;
                    count++;
                    if (r.hasCalorieEstimate()) {
                        totalKnown += r.getCalories();
                    } else {
                        unknown++;
                    }
                }
            }
        }
        return new PlannerCalorieSummary(totalKnown, unknown, count);
    }

    public int getTotalKnownCalories() {
        return totalKnownCalories;
    }

    public int getUnknownRecipeCount() {
        return unknownRecipeCount;
    }

    public int getRecipeCount() {
        return recipeCount;
    }

    public boolean shouldShowBanner() {
        return recipeCount > 0;
    }

    public Coverage getCoverage() {
        if (recipeCount == 0) return Coverage.NONE;
        if (unknownRecipeCount == 0) return Coverage.ALL_KNOWN;
        if (totalKnownCalories > 0) return Coverage.PARTIAL;
        return Coverage.ALL_UNKNOWN;
    }
}
