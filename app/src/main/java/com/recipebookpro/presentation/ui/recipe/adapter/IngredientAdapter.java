package com.recipebookpro.presentation.ui.recipe.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.recipebookpro.R;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.util.RiskyIngredientMatcher;

import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.ViewHolder> {

    private final List<Recipe.Ingredient> ingredients;
    private List<String> riskyMatchTerms;
    private double currentScaleRatio = 1.0;

    public IngredientAdapter(List<Recipe.Ingredient> ingredients, List<String> riskyMatchTerms) {
        this.ingredients = ingredients;
        this.riskyMatchTerms = riskyMatchTerms;
    }

    public void setRiskyMatchTerms(List<String> riskyMatchTerms) {
        this.riskyMatchTerms = riskyMatchTerms;
        notifyDataSetChanged();
    }

    public void setScaleRatio(double ratio) {
        this.currentScaleRatio = ratio;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe.Ingredient ingredient = ingredients.get(position);

        // Use scaled display text if a numeric amount exists
        String amountUnit = "";
        if (ingredient.getNumericAmount() > 0) {
            amountUnit = ingredient.getScaledDisplayText(currentScaleRatio);
            // Since scaled display text includes the name, we just show that in the name
            // field and hide amount if we want,
            // or we can separate them. ScaledDisplayText returns everything. Let's just put
            // it in tvIngredientName.
            holder.tvIngredientName.setText(ingredient.getScaledDisplayText(currentScaleRatio));
            holder.tvIngredientAmountUnit.setVisibility(View.GONE);
        } else {
            holder.tvIngredientName.setText(ingredient.getDisplayName());
            String au = (ingredient.getAmount() + " " + ingredient.getUnit()).trim();
            if (!au.isEmpty()) {
                holder.tvIngredientAmountUnit.setText(au);
                holder.tvIngredientAmountUnit.setVisibility(View.VISIBLE);
            } else {
                holder.tvIngredientAmountUnit.setVisibility(View.GONE);
            }
        }

        boolean hasRisk = RiskyIngredientMatcher.isRisky(ingredient, riskyMatchTerms, currentScaleRatio);

        if (hasRisk) {
            holder.allergyIndicator.setVisibility(View.VISIBLE);
            
            Context context = holder.itemView.getContext();
            TypedValue typedValue = new TypedValue();
            
            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorErrorContainer, typedValue, true);
            holder.itemView.setBackgroundColor(typedValue.data);
            
            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnErrorContainer, typedValue, true);
            holder.tvIngredientName.setTextColor(typedValue.data);
            holder.tvIngredientAmountUnit.setTextColor(typedValue.data);
        } else {
            holder.allergyIndicator.setVisibility(View.GONE);
            holder.itemView.setBackgroundResource(android.R.color.transparent);

            Context context = holder.itemView.getContext();
            TypedValue typedValue = new TypedValue();

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
            holder.tvIngredientName.setTextColor(typedValue.data);

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
            holder.tvIngredientAmountUnit.setTextColor(typedValue.data);
        }
    }

    @Override
    public int getItemCount() {
        return ingredients == null ? 0 : ingredients.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIngredientAmountUnit;
        TextView tvIngredientName;
        View allergyIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            tvIngredientAmountUnit = itemView.findViewById(R.id.tvIngredientAmountUnit);
            tvIngredientName = itemView.findViewById(R.id.tvIngredientName);
            allergyIndicator = itemView.findViewById(R.id.allergyIndicator);
        }
    }
}
