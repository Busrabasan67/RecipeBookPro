package com.recipebookpro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.recipebookpro.data.local.converter.StringListConverter;

import java.util.List;

@Entity(tableName = "users")
@TypeConverters({StringListConverter.class})
public class UserEntity {
    @PrimaryKey
    @NonNull
    private String uid;
    private String email;
    private String displayName;
    private String profileImageUrl;
    private List<String> allergens;
    private long lastUpdated;

    public UserEntity(@NonNull String uid, String email, String displayName, String profileImageUrl, List<String> allergens) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.allergens = allergens;
        this.lastUpdated = System.currentTimeMillis();
    }

    @NonNull
    public String getUid() { return uid; }
    public void setUid(@NonNull String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public List<String> getAllergens() { return allergens; }
    public void setAllergens(List<String> allergens) { this.allergens = allergens; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
