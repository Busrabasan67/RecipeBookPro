package com.recipebookpro.domain.model;

import java.util.List;

/** Tek bir gün kartı için bilinen günlük toplam ve eksik tahmin sayısı. */
public final class DayPlannerCalorieSummary {

    private final int knownDayTotal;
    private final int unknownRecipeCount;
    private final boolean hasRecipes;

    private DayPlannerCalorieSummary(int knownDayTotal, int unknownRecipeCount, boolean hasRecipes) {
        this.knownDayTotal = knownDayTotal;
        this.unknownRecipeCount = unknownRecipeCount;
        this.hasRecipes = hasRecipes;
    }

    public static DayPlannerCalorieSummary from(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return new DayPlannerCalorieSummary(0, 0, false);
        }
        int known = 0;
        int unknown = 0;
        for (Recipe r : recipes) {
            if (r == null) continue;
            if (r.hasCalorieEstimate()) {
                known += r.getCalories();
            } else {
                unknown++;
            }
        }
        return new DayPlannerCalorieSummary(known, unknown, true);
    }

    public boolean shouldHideRow() {
        return !hasRecipes;
    }

    public int getKnownDayTotal() {
        return knownDayTotal;
    }

    /** Tarif var ama hiçbirinde kalori yok. */
    public boolean isAllPending() {
        return hasRecipes && knownDayTotal == 0 && unknownRecipeCount > 0;
    }
}
