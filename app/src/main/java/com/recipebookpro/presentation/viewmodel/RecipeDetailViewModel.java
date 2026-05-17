package com.recipebookpro.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.repository.HealthRepository;
import com.recipebookpro.domain.usecase.CheckRecipeSafetyUseCase;

import java.util.List;
import java.util.Map;

public class RecipeDetailViewModel extends ViewModel {

    private final CheckRecipeSafetyUseCase checkRecipeSafetyUseCase;

    public static class HealthCheckState {
        public final boolean isSafe;
        public final String rationale;
        public final List<String> riskyIngredients;
        public final String error;
        public final boolean isLoading;

        public HealthCheckState(boolean isSafe, String rationale, List<String> riskyIngredients, String error, boolean isLoading) {
            this.isSafe = isSafe;
            this.rationale = rationale;
            this.riskyIngredients = riskyIngredients;
            this.error = error;
            this.isLoading = isLoading;
        }

        public static HealthCheckState Loading() {
            return new HealthCheckState(true, null, null, null, true);
        }

        public static HealthCheckState Success(boolean isSafe, String rationale, List<String> riskyIngredients) {
            return new HealthCheckState(isSafe, rationale, riskyIngredients, null, false);
        }

        public static HealthCheckState Error(String error) {
            return new HealthCheckState(false, null, null, error, false);
        }
    }

    private final MutableLiveData<HealthCheckState> healthCheckState = new MutableLiveData<>();

    public RecipeDetailViewModel(CheckRecipeSafetyUseCase checkRecipeSafetyUseCase) {
        this.checkRecipeSafetyUseCase = checkRecipeSafetyUseCase;
    }

    public LiveData<HealthCheckState> getHealthCheckState() {
        return healthCheckState;
    }

    public void checkRecipeSafety(Recipe recipe,
                                  List<String> healthConditions,
                                  List<String> customHealthConditions,
                                  List<String> allergens,
                                  Map<String, List<String>> healthTriggers,
                                  String uiLangCode) {
        
        healthCheckState.setValue(HealthCheckState.Loading());

        checkRecipeSafetyUseCase.execute(
                recipe,
                healthConditions,
                customHealthConditions,
                allergens,
                healthTriggers,
                uiLangCode,
                new HealthRepository.HealthCheckCallback() {
                    @Override
                    public void onResult(boolean isSafe, String rationale, List<String> riskyIngredients) {
                        healthCheckState.postValue(HealthCheckState.Success(isSafe, rationale, riskyIngredients));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        healthCheckState.postValue(HealthCheckState.Error(errorMessage));
                    }
                }
        );
    }
}
