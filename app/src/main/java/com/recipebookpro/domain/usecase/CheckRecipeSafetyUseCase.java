package com.recipebookpro.domain.usecase;

import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.repository.HealthRepository;

import java.util.List;
import java.util.Map;

public class CheckRecipeSafetyUseCase {

    private final HealthRepository healthRepository;

    public CheckRecipeSafetyUseCase(HealthRepository healthRepository) {
        this.healthRepository = healthRepository;
    }

    public void execute(Recipe recipe,
                        List<String> healthConditions,
                        List<String> customHealthConditions,
                        List<String> allergens,
                        Map<String, List<String>> healthTriggers,
                        String uiLangCode,
                        HealthRepository.HealthCheckCallback callback) {
        
        healthRepository.checkRecipeSafety(
                recipe, 
                healthConditions, 
                customHealthConditions, 
                allergens, 
                healthTriggers, 
                uiLangCode, 
                callback
        );
    }
}
