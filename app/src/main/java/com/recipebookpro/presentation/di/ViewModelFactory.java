package com.recipebookpro.presentation.di;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.recipebookpro.data.repository.RecipeRepositoryImpl;
import com.recipebookpro.data.repository.AuthRepositoryImpl;
import com.recipebookpro.domain.usecase.GetRecipesUseCase;
import com.recipebookpro.domain.usecase.GetCurrentUserUseCase;
import com.recipebookpro.presentation.viewmodel.RecipeViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {
    
    private static ViewModelFactory instance;
    
    private ViewModelFactory() {}
    
    public static ViewModelFactory getInstance() {
        if (instance == null) {
            instance = new ViewModelFactory();
        }
        return instance;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(RecipeViewModel.class)) {
            RecipeRepositoryImpl repo = new RecipeRepositoryImpl();
            AuthRepositoryImpl authRepo = new AuthRepositoryImpl();
            return (T) new RecipeViewModel(new GetRecipesUseCase(repo), new GetCurrentUserUseCase(authRepo));
        } else if (modelClass.isAssignableFrom(com.recipebookpro.presentation.viewmodel.RecipeDetailViewModel.class)) {
            com.recipebookpro.data.remote.HealthCheckService service = new com.recipebookpro.data.remote.HealthCheckService();
            com.recipebookpro.domain.repository.HealthRepository repo = new com.recipebookpro.data.repository.HealthRepositoryImpl(service);
            com.recipebookpro.domain.usecase.CheckRecipeSafetyUseCase useCase = new com.recipebookpro.domain.usecase.CheckRecipeSafetyUseCase(repo);
            return (T) new com.recipebookpro.presentation.viewmodel.RecipeDetailViewModel(useCase);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
