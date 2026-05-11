package com.recipebookpro.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.repository.RecipeRepository;
import com.recipebookpro.domain.usecase.GetRecipesUseCase;
import com.recipebookpro.domain.usecase.GetCurrentUserUseCase;

import java.util.List;

public class RecipeViewModel extends ViewModel {

    private final GetRecipesUseCase getRecipesUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final MutableLiveData<List<Recipe>> recipesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public RecipeViewModel(GetRecipesUseCase getRecipesUseCase, GetCurrentUserUseCase getCurrentUserUseCase) {
        this.getRecipesUseCase = getRecipesUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    public String getCurrentUserId() {
        return getCurrentUserUseCase.getUserId();
    }

    public String getCurrentUserEmail() {
        return getCurrentUserUseCase.getUserEmail();
    }

    public LiveData<List<Recipe>> getRecipes() {
        return recipesLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void loadRecipes(String userId) {
        loadingLiveData.setValue(true);
        getRecipesUseCase.execute(userId, new RecipeRepository.OnRecipesLoadedListener() {
            @Override
            public void onLoaded(List<Recipe> recipes) {
                loadingLiveData.setValue(false);
                recipesLiveData.setValue(recipes);
            }

            @Override
            public void onError(Exception e) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(e.getMessage());
            }
        });
    }
}
