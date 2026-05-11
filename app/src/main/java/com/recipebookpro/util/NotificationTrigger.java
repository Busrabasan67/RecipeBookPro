package com.recipebookpro.util;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.recipebookpro.data.repository.NotificationRepositoryImpl;
import com.recipebookpro.domain.model.Notification;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.model.User;
import com.recipebookpro.domain.repository.NotificationRepository;

import java.util.List;
import java.util.Map;

public class NotificationTrigger {

    private static final NotificationRepository repository = new NotificationRepositoryImpl();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void triggerNewRecipeFromUser(Recipe recipe, String userName) {
        db.collection("users").document(recipe.getUserId()).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user == null) return;
            
            List<String> followers = user.getFollowerIds();
            if (followers == null) return;

            for (String followerId : followers) {
                Notification n = new Notification();
                n.setRecipientId(followerId);
                n.setType(Notification.TYPE_NEW_RECIPE_USER);
                n.setTitle("New Recipe");
                n.setTitleTr("Yeni Tarif");
                n.setMessage(userName + " added a new recipe: " + recipe.getTitle());
                n.setMessageTr(userName + " yeni bir tarif ekledi: " + recipe.getTitle());
                n.getData().put("recipeId", recipe.getId());
                n.getData().put("userId", recipe.getUserId());
                repository.sendNotification(n);
            }
        });
    }

    public static void triggerNewRecipeInCookbook(Recipe recipe, String cookbookId, String cookbookName) {
        db.collection("cookbooks").document(cookbookId).get().addOnSuccessListener(doc -> {
            List<String> followers = (List<String>) doc.get("followerIds");
            if (followers == null) return;

            for (String followerId : followers) {
                Notification n = new Notification();
                n.setRecipientId(followerId);
                n.setType(Notification.TYPE_NEW_RECIPE_COOKBOOK);
                n.setTitle("New Recipe in Cookbook");
                n.setTitleTr("Deftere Yeni Tarif");
                n.setMessage("New recipe added to " + cookbookName + ": " + recipe.getTitle());
                n.setMessageTr(cookbookName + " defterine yeni bir tarif eklendi: " + recipe.getTitle());
                n.getData().put("recipeId", recipe.getId());
                n.getData().put("cookbookId", cookbookId);
                repository.sendNotification(n);
            }
        });
    }

    public static void triggerInvitation(String fromUserName, String toUserId, String targetName, String invitationId, String type) {
        Notification n = new Notification();
        n.setRecipientId(toUserId);
        n.setType(Notification.TYPE_INVITATION);
        n.setTitle("New Invitation");
        n.setTitleTr("Yeni Davet");
        n.setMessage(fromUserName + " invited you to " + targetName);
        n.setMessageTr(fromUserName + " sizi " + targetName + " için davet etti");
        n.getData().put("invitationId", invitationId);
        n.getData().put("type", type);
        repository.sendNotification(n);
    }
}
