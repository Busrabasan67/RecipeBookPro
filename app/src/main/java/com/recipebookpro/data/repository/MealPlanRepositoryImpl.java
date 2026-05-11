package com.recipebookpro.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.recipebookpro.domain.model.MealPlan;
import com.recipebookpro.domain.repository.MealPlanRepository;

import java.util.ArrayList;
import java.util.List;

public class MealPlanRepositoryImpl implements MealPlanRepository {

    private final FirebaseFirestore db;
    private static final String COLLECTION_MEAL_PLANS = "meal_plans";

    public MealPlanRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public void getUserMealPlans(String userId, OnMealPlansLoadedListener listener) {
        // Query owned plans
        db.collection(COLLECTION_MEAL_PLANS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(ownerDocs -> {
                    List<MealPlan> allPlans = new ArrayList<>();
                    for (DocumentSnapshot doc : ownerDocs) {
                        allPlans.add(MealPlan.fromDocument(doc));
                    }
                    
                    // Query collaborated plans
                    db.collection(COLLECTION_MEAL_PLANS)
                            .whereArrayContains("collaboratorIds", userId)
                            .get()
                            .addOnSuccessListener(collabDocs -> {
                                for (DocumentSnapshot doc : collabDocs) {
                                    MealPlan p = MealPlan.fromDocument(doc);
                                    // Avoid duplicates if user is somehow both owner and collab
                                    boolean exists = false;
                                    for (MealPlan existing : allPlans) {
                                        if (existing.getId().equals(p.getId())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) allPlans.add(p);
                                }
                                
                                // Sort by createdAt descending
                                allPlans.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                                listener.onLoaded(allPlans);
                            })
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void saveMealPlan(MealPlan plan, OnMealPlanActionCompleteListener listener) {
        String id = plan.getId();
        if (id == null || id.isEmpty()) {
            id = db.collection(COLLECTION_MEAL_PLANS).document().getId();
            plan.setId(id);
        }
        
        db.collection(COLLECTION_MEAL_PLANS).document(id)
                .set(plan)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void deleteMealPlan(String planId, OnMealPlanActionCompleteListener listener) {
        db.collection(COLLECTION_MEAL_PLANS).document(planId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
}
