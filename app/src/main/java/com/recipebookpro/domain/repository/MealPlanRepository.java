package com.recipebookpro.domain.repository;

import com.recipebookpro.domain.model.MealPlan;
import java.util.List;

public interface MealPlanRepository {
    interface OnMealPlansLoadedListener {
        void onLoaded(List<MealPlan> plans);
        void onError(Exception e);
    }

    interface OnMealPlanActionCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }

    void getUserMealPlans(String userId, OnMealPlansLoadedListener listener);
    void saveMealPlan(MealPlan plan, OnMealPlanActionCompleteListener listener);
    void deleteMealPlan(String planId, OnMealPlanActionCompleteListener listener);
}
