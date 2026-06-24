package com.recipebookpro.presentation.ui.planner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.DayPlannerCalorieSummary;
import com.recipebookpro.domain.model.Recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DayCardAdapter extends RecyclerView.Adapter<DayCardAdapter.DayViewHolder> {

    public interface OnDayInteractionListener {
        void onAddRecipeClick(String dayKey);
        void onRecipeLongPress(String dayKey, Recipe recipe);
        void onRecipeClick(Recipe recipe);
    }

    private final Map<String, List<Recipe>> dayRecipesMap = new HashMap<>();
    private final OnDayInteractionListener listener;
    private int duration = 7;

    public DayCardAdapter(OnDayInteractionListener listener) {
        this.listener = listener;
    }

    public void setDuration(int duration) {
        this.duration = duration;
        notifyDataSetChanged();
    }

    public void setDayRecipes(String dayKey, List<Recipe> recipes) {
        dayRecipesMap.put(dayKey, recipes != null ? recipes : new ArrayList<>());
        int index = dayIndex(dayKey);
        if (index >= 0) notifyItemChanged(index);
    }

    public void setAllDayRecipes(Map<String, List<Recipe>> allData) {
        dayRecipesMap.clear();
        dayRecipesMap.putAll(allData);
        notifyDataSetChanged();
    }

    private int dayIndex(String dayKey) {
        try {
            return Integer.parseInt(dayKey.replace("day_", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_card, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String dayKey = "day_" + position;
        String dayLabel;
        if (duration == 7) {
            String[] dayLabels = holder.itemView.getResources().getStringArray(R.array.week_day_labels);
            dayLabel = position < dayLabels.length ? dayLabels[position] : holder.itemView.getContext().getString(R.string.day_n, (position + 1));
        } else {
            dayLabel = holder.itemView.getContext().getString(R.string.day_n, (position + 1));
        }
        
        List<Recipe> recipes = dayRecipesMap.get(dayKey);
        if (recipes == null) recipes = new ArrayList<>();

        holder.tvDayName.setText(dayLabel);
        
        DayPlannerCalorieSummary daySummary = DayPlannerCalorieSummary.from(recipes);

        if (daySummary.shouldHideRow()) {
            holder.tvDayCalories.setVisibility(View.GONE);
        } else if (daySummary.isAllPending()) {
            holder.tvDayCalories.setVisibility(View.VISIBLE);
            holder.tvDayCalories.setAlpha(0.85f);
            holder.tvDayCalories.setTextColor(
                    MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurfaceVariant));
            holder.tvDayCalories.setText(holder.itemView.getContext().getString(R.string.day_calories_unknown));
        } else {
            holder.tvDayCalories.setVisibility(View.VISIBLE);
            holder.tvDayCalories.setAlpha(1f);
            holder.tvDayCalories.setTextColor(MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorTertiary));
            holder.tvDayCalories.setText(daySummary.getKnownDayTotal() + " kcal");
        }
        holder.tvDayEmpty.setVisibility(recipes.isEmpty() ? View.VISIBLE : View.GONE);
        holder.rvDayRecipes.setVisibility(recipes.isEmpty() ? View.GONE : View.VISIBLE);

        DayRecipeAdapter recipeAdapter = new DayRecipeAdapter(recipes, recipe -> {
            if (listener != null) listener.onRecipeClick(recipe);
        });
        holder.rvDayRecipes.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.rvDayRecipes.setAdapter(recipeAdapter);

        recipeAdapter.setOnLongClickListener(recipe -> {
            if (listener != null) listener.onRecipeLongPress(dayKey, recipe);
        });

        holder.btnAddToDay.setOnClickListener(v -> {
            if (listener != null) listener.onAddRecipeClick(dayKey);
        });
    }

    @Override
    public int getItemCount() {
        return duration;
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvDayName, tvDayEmpty, tvDayCalories;
        RecyclerView rvDayRecipes;
        View btnAddToDay;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvDayEmpty = itemView.findViewById(R.id.tvDayEmpty);
            tvDayCalories = itemView.findViewById(R.id.tvDayCalories);
            rvDayRecipes = itemView.findViewById(R.id.rvDayRecipes);
            btnAddToDay = itemView.findViewById(R.id.btnAddToDay);
        }
    }
}
