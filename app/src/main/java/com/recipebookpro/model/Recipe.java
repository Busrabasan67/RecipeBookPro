package com.recipebookpro.model;

import java.util.List;

public class Recipe {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String ingredients;
    private String steps;
    private long createdAt;
    private List<String> stickerAssetPaths; // Phase 4 için hazır

    public Recipe() {
        // Firestore requires empty constructor
    }

    public Recipe(String id, String userId, String title, String description,
                  String ingredients, String steps, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.ingredients = ingredients;
        this.steps = steps;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public List<String> getStickerAssetPaths() { return stickerAssetPaths; }
    public void setStickerAssetPaths(List<String> stickerAssetPaths) { this.stickerAssetPaths = stickerAssetPaths; }
}
