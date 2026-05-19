package com.recipebookpro.presentation.ui.recipe.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.presentation.ui.recipe.IngredientsTabFragment;
import com.recipebookpro.presentation.ui.recipe.NotesTabFragment;
import com.recipebookpro.presentation.ui.recipe.StepsTabFragment;

import java.util.ArrayList;

public class RecipeDetailPagerAdapter extends FragmentStateAdapter {

    private final Recipe recipe;


    public RecipeDetailPagerAdapter(@NonNull FragmentActivity fragmentActivity, Recipe recipe) {
        super(fragmentActivity);
        this.recipe = recipe;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return IngredientsTabFragment.newInstance(recipe);
            case 1:
                return StepsTabFragment.newInstance(recipe);
            case 2:
                return NotesTabFragment.newInstance(recipe);
            default:
                return IngredientsTabFragment.newInstance(recipe);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
