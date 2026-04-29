package com.recipebookpro.domain.usecase;

import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.repository.RecipeRepository;

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
        recipeRepository.getRecipesByUserId(userId, listener);
    }
}
