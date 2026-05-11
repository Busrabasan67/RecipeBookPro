package com.recipebookpro.domain.usecase;

import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.nutrition.EstimatedCaloriesParser;
import com.recipebookpro.domain.repository.RecipeRepository;
import com.recipebookpro.domain.service.AiNutritionService;

public class AnalyzeRecipeCaloriesUseCase {

    private static final int PARSE_MISS = -1;

    private final RecipeRepository recipeRepository;
    private final AiNutritionService aiNutritionService;

    public AnalyzeRecipeCaloriesUseCase(RecipeRepository recipeRepository, AiNutritionService aiNutritionService) {
        this.recipeRepository = recipeRepository;
        this.aiNutritionService = aiNutritionService;
    }

    public interface AnalyzeCallback {
        void onSuccess(int calories);

        void onError(String error);
    }

    public void execute(Recipe recipe, AnalyzeCallback callback) {
        if (recipe == null || recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            callback.onError("No ingredients to analyze");
            return;
        }

        aiNutritionService.estimateTotalCaloriesForRecipe(recipe, new AiNutritionService.ResultCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    int cal = EstimatedCaloriesParser.parseTotalKcalFlexible(result, PARSE_MISS);
                    if (cal > 0) {
                        recipe.setCalories(cal);
                        recipeRepository.updateRecipeCalories(recipe.getId(), cal);
                        callback.onSuccess(cal);
                    } else {
                        callback.onError("Invalid calorie calculation: " + result);
                    }
                } catch (Exception e) {
                    callback.onError("Failed to parse calories: " + result);
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
