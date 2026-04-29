package com.recipebookpro.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.repository.RecipeRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeRepositoryImpl implements RecipeRepository {

    private final FirebaseFirestore db;

    public RecipeRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public void getRecipesByUserId(String userId, OnRecipesLoadedListener listener) {
        db.collection("recipes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) {
                    listener.onError(error != null ? error : new Exception("Unknown error"));
                    return;
                }

                List<Recipe> allRecipes = new ArrayList<>();
                Set<String> addedIds = new HashSet<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    if (addedIds.add(doc.getId())) {
                        allRecipes.add(Recipe.fromDocument(doc));
                    }
                }
                
                Collections.sort(allRecipes, (r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
                listener.onLoaded(allRecipes);
            });
    }
}
