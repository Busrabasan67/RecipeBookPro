package com.recipebookpro.domain.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MealPlan implements Serializable {

    private String id;
    private String userId;
    private String name;
    private int duration; // 3, 7, 10
    private long createdAt;
    private Map<String, List<String>> days; // Key: "day_0", "day_1", etc.
    private List<String> collaboratorIds;
    private int totalCalories;

    public MealPlan() {
        this.days = new HashMap<>();
        this.collaboratorIds = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public MealPlan(String userId, String name, int duration) {
        this.userId = userId;
        this.name = name;
        this.duration = duration;
        this.createdAt = System.currentTimeMillis();
        this.days = new HashMap<>();
        this.collaboratorIds = new ArrayList<>();
        
        for (int i = 0; i < duration; i++) {
            days.put("day_" + i, new ArrayList<>());
        }
    }

    @SuppressWarnings("unchecked")
    public static MealPlan fromDocument(DocumentSnapshot doc) {
        MealPlan plan = new MealPlan();
        plan.setId(doc.getId());
        plan.setUserId(doc.getString("userId"));
        plan.setName(doc.getString("name"));
        
        Object durationVal = doc.get("duration");
        if (durationVal instanceof Number) {
            plan.setDuration(((Number) durationVal).intValue());
        }

        Object createdVal = doc.get("createdAt");
        if (createdVal instanceof Number) {
            plan.setCreatedAt(((Number) createdVal).longValue());
        }

        Object caloriesVal = doc.get("totalCalories");
        if (caloriesVal instanceof Number) {
            plan.setTotalCalories(((Number) caloriesVal).intValue());
        }

        Object collabVal = doc.get("collaboratorIds");
        if (collabVal instanceof List<?>) {
            plan.setCollaboratorIds((List<String>) collabVal);
        }

        Object daysVal = doc.get("days");
        if (daysVal instanceof Map<?, ?>) {
            Map<?, ?> rawDays = (Map<?, ?>) daysVal;
            Map<String, List<String>> parsedDays = new HashMap<>();
            for (Object key : rawDays.keySet()) {
                Object list = rawDays.get(key);
                if (list instanceof List<?>) {
                    parsedDays.put(String.valueOf(key), (List<String>) list);
                }
            }
            plan.setDays(parsedDays);
        }

        return plan;
    }

    @Exclude
    public List<String> getAllRecipeIds() {
        List<String> all = new ArrayList<>();
        if (days != null) {
            for (List<String> dayRecipes : days.values()) {
                if (dayRecipes != null) {
                    for (String id : dayRecipes) {
                        if (!all.contains(id)) {
                            all.add(id);
                        }
                    }
                }
            }
        }
        return all;
    }

    @Exclude
    public List<String> getAllRecipeIdsWithDuplicates() {
        List<String> all = new ArrayList<>();
        if (days != null) {
            for (List<String> dayRecipes : days.values()) {
                if (dayRecipes != null) {
                    all.addAll(dayRecipes);
                }
            }
        }
        return all;
    }

    // ========================== Getters & Setters ==========================

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, List<String>> getDays() { return days; }
    public void setDays(Map<String, List<String>> days) { this.days = days; }

    public List<String> getCollaboratorIds() { return collaboratorIds == null ? new ArrayList<>() : collaboratorIds; }
    public void setCollaboratorIds(List<String> collaboratorIds) { this.collaboratorIds = collaboratorIds; }

    public int getTotalCalories() { return totalCalories; }
    public void setTotalCalories(int totalCalories) { this.totalCalories = totalCalories; }
}
