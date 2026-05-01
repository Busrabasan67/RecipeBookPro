package com.recipebookpro.presentation.ui.planner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.Recipe;

import java.util.List;

public class DayRecipeAdapter extends RecyclerView.Adapter<DayRecipeAdapter.ViewHolder> {

    private final List<Recipe> recipes;
    private final OnRecipeClickListener clickListener;
    private OnRecipeLongClickListener longClickListener;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    public interface OnRecipeLongClickListener {
        void onRecipeLongClick(Recipe recipe);
    }

    public DayRecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.clickListener = listener;
    }

    public void setOnLongClickListener(OnRecipeLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        String currentLang = java.util.Locale.getDefault().getLanguage();
        holder.tvRecipeName.setText(recipe.getDisplayTitle(currentLang));

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onRecipeClick(recipe);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onRecipeLongClick(recipe);
                return true;
            }
            return false;
        });

        holder.btnRemoveDayRecipe.setOnClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onRecipeLongClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvRecipeName;
        View btnRemoveDayRecipe;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecipeName = itemView.findViewById(R.id.tvDayRecipeName);
            btnRemoveDayRecipe = itemView.findViewById(R.id.btnRemoveDayRecipe);
        }
    }
}
