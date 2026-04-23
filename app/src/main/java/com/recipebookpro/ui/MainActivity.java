package com.recipebookpro.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.recipebookpro.R;
import com.recipebookpro.adapter.RecipeAdapter;
import com.recipebookpro.model.Recipe;
import com.recipebookpro.ui.auth.LoginActivity;
import com.recipebookpro.ui.recipe.AddRecipeActivity;
import com.recipebookpro.ui.recipe.RecipeDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeClickListener {

    private RecyclerView rvRecipes;
    private MaterialTextView tvEmpty, tvSubtitle;
    private RecipeAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration recipeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvRecipes = findViewById(R.id.rvRecipes);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddRecipe);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvSubtitle.setText(currentUser.getEmail());
        }

        adapter = new RecipeAdapter(this);
        rvRecipes.setLayoutManager(new LinearLayoutManager(this));
        rvRecipes.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddRecipeActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear any old data that might be cached while the activity was paused.
        if (adapter != null) {
            adapter.setRecipeList(new ArrayList<>());
        }
        listenForRecipes();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recipeListener != null) {
            recipeListener.remove();
            recipeListener = null;
        }
    }

    private void listenForRecipes() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        recipeListener = db.collection("recipes")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots == null) return;

                    List<Recipe> recipes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        recipe.setId(doc.getId());
                        recipes.add(recipe);
                    }

                    adapter.setRecipeList(recipes);

                    if (recipes.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvRecipes.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvRecipes.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_TITLE, recipe.getTitle());
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_DESC, recipe.getDescription());
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_INGREDIENTS, recipe.getIngredients());
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_STEPS, recipe.getSteps());
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_DATE, recipe.getCreatedAt());
        startActivity(intent);
    }
}
