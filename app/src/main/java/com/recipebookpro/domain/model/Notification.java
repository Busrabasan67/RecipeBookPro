package com.recipebookpro.domain.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Notification implements Serializable {

    public static final String TYPE_INVITATION = "invitation";
    public static final String TYPE_NEW_RECIPE_USER = "new_recipe_user";
    public static final String TYPE_NEW_RECIPE_COOKBOOK = "new_recipe_cookbook";

    private String id;
    private String recipientId;
    private String type;
    private String title;
    private String titleTr;
    private String message;
    private String messageTr;
    private long timestamp;
    private boolean isRead;
    private Map<String, String> data;

    public Notification() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.data = new HashMap<>();
    }

    public static Notification fromDocument(DocumentSnapshot doc) {
        Notification notification = doc.toObject(Notification.class);
        if (notification != null) {
            notification.setId(doc.getId());
        }
        return notification;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTitleTr() { return titleTr; }
    public void setTitleTr(String titleTr) { this.titleTr = titleTr; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMessageTr() { return messageTr; }
    public void setMessageTr(String messageTr) { this.messageTr = messageTr; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }
}
