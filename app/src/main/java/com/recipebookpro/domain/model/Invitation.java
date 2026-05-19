package com.recipebookpro.domain.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class Invitation implements Serializable {

    public static final String TYPE_COOKBOOK = "cookbook";
    public static final String TYPE_MEAL_PLAN = "meal_plan";
    public static final String TYPE_SHOPPING_LIST = "shopping_list";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_DISMISSED = "dismissed";

    private String id;
    private String type;
    private String targetId; // cookbookId, mealPlanId, or shoppingListId
    private String targetName;
    private String fromUserId;
    private String fromUserName;
    private String toUserId;
    private String toUserEmail;
    private String status;
    private long createdAt;

    public Invitation() {
        this.status = STATUS_PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public static Invitation fromDocument(DocumentSnapshot doc) {
        Invitation invitation = new Invitation();
        invitation.setId(doc.getId());
        invitation.setType(doc.getString("type"));
        invitation.setTargetId(doc.getString("targetId"));
        invitation.setTargetName(doc.getString("targetName"));
        invitation.setFromUserId(doc.getString("fromUserId"));
        invitation.setFromUserName(doc.getString("fromUserName"));
        invitation.setToUserId(doc.getString("toUserId"));
        invitation.setToUserEmail(doc.getString("toUserEmail"));
        invitation.setStatus(doc.getString("status"));
        
        Object createdVal = doc.get("createdAt");
        if (createdVal instanceof Number) {
            invitation.setCreatedAt(((Number) createdVal).longValue());
        }
        
        return invitation;
    }

    // ========================== Getters & Setters ==========================

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getToUserEmail() { return toUserEmail; }
    public void setToUserEmail(String toUserEmail) { this.toUserEmail = toUserEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
