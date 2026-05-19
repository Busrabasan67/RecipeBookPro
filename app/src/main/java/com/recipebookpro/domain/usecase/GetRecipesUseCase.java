package com.recipebookpro.domain.usecase;

import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.repository.RecipeRepository;
import com.recipebookpro.domain.service.RecipeFamilyDeduplicator;

import java.util.Collections;
import java.util.List;

public class GetRecipesUseCase {
    private final RecipeRepository recipeRepository;

    public GetRecipesUseCase(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public void execute(String userId, RecipeRepository.OnRecipesLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onError(new IllegalArgumentException("User ID cannot be null"));
            return;
        }
        recipeRepository.getRecipesByUserId(userId, new RecipeRepository.OnRecipesLoadedListener() {
            @Override
            public void onLoaded(List<Recipe> recipes) {
                List<Recipe> deduplicatedRecipes = RecipeFamilyDeduplicator.keepLatestRecipePerFamily(recipes);
                Collections.sort(deduplicatedRecipes,
                        (r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                listener.onLoaded(deduplicatedRecipes);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }
}
