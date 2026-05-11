package com.recipebookpro.domain.usecase;

import com.recipebookpro.domain.model.MealPlan;
import com.recipebookpro.domain.repository.MealPlanRepository;

/**
 * Yalnızca planın sahibi olan kullanıcı silebilir (iş ortağı listelerinden kaldırma değildir).
 */
public class DeleteMealPlanUseCase {

    private final MealPlanRepository mealPlanRepository;

    public DeleteMealPlanUseCase(MealPlanRepository mealPlanRepository) {
        this.mealPlanRepository = mealPlanRepository;
    }

    public void execute(MealPlan plan, String currentUserId, MealPlanRepository.OnMealPlanActionCompleteListener listener) {
        if (plan == null || plan.getId() == null || plan.getId().isEmpty()) {
            listener.onError(new IllegalArgumentException("Invalid meal plan"));
            return;
        }
        if (currentUserId == null || currentUserId.isEmpty()) {
            listener.onError(new IllegalArgumentException("Not signed in"));
            return;
        }
        if (!currentUserId.equals(plan.getUserId())) {
            listener.onError(new SecurityException("Only the plan owner can delete"));
            return;
        }
        mealPlanRepository.deleteMealPlan(plan.getId(), listener);
    }
}
