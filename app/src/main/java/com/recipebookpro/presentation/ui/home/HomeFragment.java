package com.recipebookpro.presentation.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import com.recipebookpro.R;
import com.recipebookpro.presentation.adapter.RecipeAdapter;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.presentation.di.ViewModelFactory;
import com.recipebookpro.presentation.ui.book.BookReaderActivity;
import com.recipebookpro.presentation.ui.recipe.RecipeAddEditActivity;
import com.recipebookpro.presentation.ui.recipe.RecipeDetailActivity;
import com.recipebookpro.presentation.viewmodel.RecipeViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {

    private static final String FILTER_ALL = "__ALL__";

    private RecyclerView rvRecipes;
    private MaterialTextView tvEmpty;
    private MaterialTextView tvSubtitle;
    private ChipGroup chipGroupCategories;
    private CircularProgressIndicator progressIndicator;
    private View rootView;
    private RecipeAdapter adapter;
    
    private RecipeViewModel recipeViewModel;
    
    private final List<Recipe> allRecipes = new ArrayList<>();
    private String selectedCategory = FILTER_ALL;
    private String selectedCategoryLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootView = view.findViewById(R.id.homeRoot);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        rvRecipes = view.findViewById(R.id.rvRecipes);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        progressIndicator = view.findViewById(R.id.progressRecipes);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddRecipe);
        ExtendedFloatingActionButton btnOpenBook = view.findViewById(R.id.btnOpenBook);

        recipeViewModel = new ViewModelProvider(this, ViewModelFactory.getInstance()).get(RecipeViewModel.class);

        String email = recipeViewModel.getCurrentUserEmail();
        if (email != null) {
            tvSubtitle.setText(email);
        }

        adapter = new RecipeAdapter(this);
        rvRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecipes.setAdapter(adapter);
        selectedCategoryLabel = getString(R.string.category_all);

        setupCategoryFilters();
        setupObservers();

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RecipeAddEditActivity.class)));
        btnOpenBook.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BookReaderActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        String userId = recipeViewModel.getCurrentUserId();
        if (userId != null) {
            recipeViewModel.loadRecipes(userId);
        }
    }

    private void setupObservers() {
        recipeViewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                tvEmpty.setVisibility(View.GONE);
                rvRecipes.setVisibility(View.GONE);
            }
        });

        recipeViewModel.getRecipes().observe(getViewLifecycleOwner(), recipes -> {
            allRecipes.clear();
            if (recipes != null) {
                allRecipes.addAll(recipes);
            }
            applyFilter();
        });

        recipeViewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && rootView != null) {
                Snackbar.make(rootView, R.string.recipes_load_failed, Snackbar.LENGTH_LONG).show();
            }
            updateEmptyState(new ArrayList<>());
        });
    }

    private void setupCategoryFilters() {
        chipGroupCategories.removeAllViews();
        addCategoryChip(getString(R.string.category_all), FILTER_ALL, true);
        String[] categoryValues = getResources().getStringArray(R.array.recipe_category_values);
        String[] categoryLabels = getResources().getStringArray(R.array.recipe_category_labels);
        int count = Math.min(categoryValues.length, categoryLabels.length);
        for (int i = 0; i < count; i++) {
            addCategoryChip(categoryLabels[i], categoryValues[i], false);
        }
    }

    private void addCategoryChip(String text, String value, boolean checked) {
        Chip chip = (Chip) LayoutInflater.from(requireContext())
                .inflate(R.layout.item_category_chip, chipGroupCategories, false);
        chip.setId(View.generateViewId());
        chip.setText(text);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedCategory = value;
                selectedCategoryLabel = text;
                applyFilter();
            }
        });
        chipGroupCategories.addView(chip);
        chip.setChecked(checked);
    }

    private void applyFilter() {
        List<Recipe> filteredRecipes = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            if (FILTER_ALL.equals(selectedCategory)
                    || selectedCategory.equalsIgnoreCase(recipe.getCategory())) {
                filteredRecipes.add(recipe);
            }
        }
        adapter.setRecipeList(filteredRecipes);
        updateEmptyState(filteredRecipes);
    }

    private void updateEmptyState(List<Recipe> recipes) {
        boolean isEmpty = recipes == null || recipes.isEmpty();
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvRecipes.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmpty.setText(FILTER_ALL.equals(selectedCategory)
                ? getString(R.string.no_recipes)
                : getString(R.string.no_recipes_for_category, selectedCategoryLabel));
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe);
        startActivity(intent);
    }
}
