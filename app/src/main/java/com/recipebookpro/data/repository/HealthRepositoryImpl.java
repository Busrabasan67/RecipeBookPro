package com.recipebookpro.data.repository;

import com.recipebookpro.data.remote.HealthCheckService;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.repository.HealthRepository;

import java.util.List;
import java.util.Map;

public class HealthRepositoryImpl implements HealthRepository {

    private final HealthCheckService healthCheckService;

    public HealthRepositoryImpl(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @Override
    public void checkRecipeSafety(Recipe recipe,
                                  List<String> healthConditions,
                                  List<String> customHealthConditions,
                                  List<String> allergens,
                                  Map<String, List<String>> healthTriggers,
                                  String uiLangCode,
                                  HealthCheckCallback callback) {

        // Delegate to remote data source (HealthCheckService)
        // We map the Domain callback to the Remote callback
        healthCheckService.checkRecipeSafety(
                recipe,
                healthConditions,
                customHealthConditions,
                allergens,
                healthTriggers,
                uiLangCode,
                new HealthCheckService.HealthCheckCallback() {
                    @Override
                    public void onResult(boolean isSafe, String rationale, List<String> riskyIngredients) {
                        callback.onResult(isSafe, rationale, riskyIngredients);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError(errorMessage);
                    }
                }
        );
    }
}
