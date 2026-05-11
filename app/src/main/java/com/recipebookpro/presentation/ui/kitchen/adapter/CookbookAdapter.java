package com.recipebookpro.presentation.ui.kitchen.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.Cookbook;

import java.util.List;

import coil.Coil;
import coil.request.ImageRequest;

public class CookbookAdapter extends RecyclerView.Adapter<CookbookAdapter.ViewHolder> {
    
    private final List<Cookbook> cookbooks;
    private final OnCookbookClickListener listener;
    private boolean isHorizontal = false;

    public interface OnCookbookClickListener {
        void onCookbookClick(Cookbook cookbook);
    }

    public CookbookAdapter(List<Cookbook> cookbooks, OnCookbookClickListener listener) {
        this.cookbooks = cookbooks;
        this.listener = listener;
    }

    public void setHorizontal(boolean horizontal) {
        this.isHorizontal = horizontal;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHorizontal ? R.layout.item_cookbook_horizontal : R.layout.item_cookbook;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cookbook book = cookbooks.get(position);
        holder.tvCookbookName.setText(book.getName());
        int count = book.getRecipeIds() != null ? book.getRecipeIds().size() : 0;
        holder.tvRecipeCount.setText(holder.itemView.getContext().getString(R.string.recipe_count, count));
        
        if (isHorizontal) {
            if (holder.tvCookbookDesc != null) {
                if (!TextUtils.isEmpty(book.getDescription())) {
                    holder.tvCookbookDesc.setText(book.getDescription());
                    holder.tvCookbookDesc.setVisibility(View.VISIBLE);
                } else {
                    holder.tvCookbookDesc.setVisibility(View.GONE);
                }
            }
            if (holder.chipFollowers != null) {
                holder.chipFollowers.setText(String.valueOf(book.getFollowerCount()));
            }
        } else {
            if (holder.tvCookbookLikes != null) {
                holder.tvCookbookLikes.setText(holder.itemView.getContext().getString(R.string.followers_count, book.getFollowerCount()));
            }
        }
        
        // Determine data to load: URL or placeholder resource
        Object imageData = (book.getCoverImageUrl() != null && !book.getCoverImageUrl().isEmpty()) 
                ? book.getCoverImageUrl() 
                : R.drawable.ic_book;
        boolean isRealImage = imageData instanceof String;

        android.util.TypedValue typedValue = new android.util.TypedValue();
        android.content.Context context = holder.itemView.getContext();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true);
        int bgColor = typedValue.data;
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimaryContainer, typedValue, true);
        int tintColor = typedValue.data;

        ImageRequest request = new ImageRequest.Builder(context)
                .data(imageData)
                .target(new coil.target.Target() {
                    @Override
                    public void onStart(@Nullable android.graphics.drawable.Drawable placeholder) {}

                    @Override
                    public void onSuccess(@NonNull android.graphics.drawable.Drawable result) {
                        if (isRealImage) {
                            holder.ivCookbookCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            holder.ivCookbookCover.setBackground(null);
                            holder.ivCookbookCover.setImageTintList(null);
                            holder.ivCookbookCover.setPadding(0, 0, 0, 0);
                            holder.ivCookbookCover.setImageDrawable(result);
                        } else {
                            holder.ivCookbookCover.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            holder.ivCookbookCover.setBackgroundColor(bgColor);
                            holder.ivCookbookCover.setPadding(0, 0, 0, 0);
                            
                            android.graphics.drawable.Drawable tinted = result.mutate();
                            androidx.core.graphics.drawable.DrawableCompat.setTint(tinted, tintColor);
                            holder.ivCookbookCover.setImageDrawable(tinted);
                        }
                    }

                    @Override
                    public void onError(@Nullable android.graphics.drawable.Drawable error) {
                        holder.ivCookbookCover.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        holder.ivCookbookCover.setImageResource(R.drawable.ic_book);
                        holder.ivCookbookCover.setBackgroundColor(bgColor);
                        holder.ivCookbookCover.setImageTintList(android.content.res.ColorStateList.valueOf(tintColor));
                    }
                })
                .crossfade(true)
                .build();
        Coil.imageLoader(context).enqueue(request);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCookbookClick(book);
        });
    }

    @Override
    public int getItemCount() {
        return cookbooks == null ? 0 : cookbooks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCookbookName, tvRecipeCount, tvCookbookLikes, tvCookbookDesc;
        ImageView ivCookbookCover;
        Chip chipFollowers;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCookbookName = itemView.findViewById(R.id.tvCookbookName);
            tvRecipeCount = itemView.findViewById(R.id.tvRecipeCount);
            tvCookbookLikes = itemView.findViewById(R.id.tvCookbookLikes);
            tvCookbookDesc = itemView.findViewById(R.id.tvCookbookDesc);
            ivCookbookCover = itemView.findViewById(R.id.ivCookbookCover);
            chipFollowers = itemView.findViewById(R.id.chipFollowerCount);
        }
    }
}
