package com.recipebookpro.domain.model;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents a single preparation step in a recipe.
 * Supports optional timer and step image.
 */
public class Step implements Serializable {

    private int order;
    private String description;
    private int timerMinutes;   // 0 means no timer
    private String imageUrl;    // optional step image
    private String translatedDescription;

    public Step() {
    }

    public Step(int order, String description, int timerMinutes, String imageUrl) {
        this.order = order;
        this.description = description;
        this.timerMinutes = timerMinutes;
        this.imageUrl = imageUrl;
    }

    /**
     * Parse a Step from a Firestore map.
     */
    public static Step fromMap(Map<?, ?> map) {
        Step step = new Step();
        Object orderVal = map.get("order");
        if (orderVal instanceof Number) {
            step.setOrder(((Number) orderVal).intValue());
        }
        Object descVal = map.get("description");
        step.setDescription(descVal != null ? String.valueOf(descVal).trim() : "");

        Object timerVal = map.get("timerMinutes");
        if (timerVal instanceof Number) {
            step.setTimerMinutes(((Number) timerVal).intValue());
        }
        Object imgVal = map.get("imageUrl");
        step.setImageUrl(imgVal != null ? String.valueOf(imgVal).trim() : "");
        
        Object transDesc = map.get("translatedDescription");
        step.setTranslatedDescription(transDesc != null ? String.valueOf(transDesc).trim() : "");
        
        return step;
    }

    // --- Getters & Setters ---

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTimerMinutes() {
        return timerMinutes;
    }

    public void setTimerMinutes(int timerMinutes) {
        this.timerMinutes = timerMinutes;
    }

    public String getImageUrl() {
        return imageUrl == null ? "" : imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean hasTimer() {
        return timerMinutes > 0;
    }

    public String getTranslatedDescription() { return translatedDescription == null ? "" : translatedDescription; }
    public void setTranslatedDescription(String translatedDescription) { this.translatedDescription = translatedDescription; }
    
    public String getDisplayDescription() {
        return (translatedDescription != null && !translatedDescription.isEmpty()) ? translatedDescription : getDescription();
    }
}
