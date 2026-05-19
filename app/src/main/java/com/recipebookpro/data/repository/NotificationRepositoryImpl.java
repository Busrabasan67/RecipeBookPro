package com.recipebookpro.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.recipebookpro.domain.model.Invitation;
import com.recipebookpro.domain.model.Notification;
import com.recipebookpro.domain.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRepositoryImpl implements NotificationRepository {

    private final FirebaseFirestore db;
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String COLLECTION_INVITATIONS = "invitations";

    public NotificationRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public LiveData<List<Notification>> getNotifications(String userId) {
        MutableLiveData<List<Notification>> liveData = new MutableLiveData<>();
        
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientId", userId)
                .addSnapshotListener((notifValue, notifError) -> {
                    db.collection(COLLECTION_INVITATIONS)
                            .whereEqualTo("toUserId", userId)
                            .whereEqualTo("status", "pending")
                            .addSnapshotListener((invValue, invError) -> {
                                List<Notification> all = new ArrayList<>();
                                
                                if (notifValue != null) {
                                    for (DocumentSnapshot doc : notifValue.getDocuments()) {
                                        all.add(Notification.fromDocument(doc));
                                    }
                                }
                                
                                if (invValue != null) {
                                    for (DocumentSnapshot doc : invValue.getDocuments()) {
                                        Invitation inv = Invitation.fromDocument(doc);
                                        
                                        // Check for duplicates (if a notification doc already exists for this invitation)
                                        boolean exists = false;
                                        for (Notification n : all) {
                                            if (n.getData() != null && doc.getId().equals(n.getData().get("invitationId"))) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        
                                        if (!exists) {
                                            all.add(mapInvitationToNotification(inv));
                                        }
                                    }
                                }
                                
                                Collections.sort(all, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                                liveData.setValue(all);
                            });
                });
        return liveData;
    }

    private Notification mapInvitationToNotification(Invitation inv) {
        Notification n = new Notification();
        n.setId(inv.getId()); // Use invitation ID directly
        n.setRecipientId(inv.getToUserId());
        n.setType(Notification.TYPE_INVITATION);
        n.setTimestamp(inv.getCreatedAt());
        
        Map<String, String> data = new HashMap<>();
        data.put("invitationId", inv.getId());
        data.put("type", inv.getType());
        n.setData(data);
        
        n.setTitle("New Invitation");
        n.setTitleTr("Yeni Davet");
        n.setMessage(inv.getFromUserName() + " invited you to " + inv.getTargetName());
        n.setMessageTr(inv.getFromUserName() + " sizi " + inv.getTargetName() + " için davet etti");
        
        return n;
    }

    @Override
    public void markAsRead(String notificationId) {
        db.collection(COLLECTION_NOTIFICATIONS).document(notificationId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        doc.getReference().update("isRead", true);
                    }
                });
    }

    @Override
    public void deleteNotification(Notification notification) {
        if (notification == null || notification.getId() == null || notification.getId().trim().isEmpty()) {
            return;
        }

        String invitationId = getInvitationId(notification);
        String notificationId = notification.getId();
        db.collection(COLLECTION_NOTIFICATIONS).document(notificationId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        doc.getReference().delete();
                    }
                    dismissInvitationIfNeeded(invitationId);
                });
    }

    private String getInvitationId(Notification notification) {
        if (!Notification.TYPE_INVITATION.equals(notification.getType())) {
            return null;
        }
        if (notification.getData() != null) {
            String dataInvitationId = notification.getData().get("invitationId");
            if (dataInvitationId != null && !dataInvitationId.trim().isEmpty()) {
                return dataInvitationId.trim();
            }
        }
        return notification.getId();
    }

    private void dismissInvitationIfNeeded(String invitationId) {
        if (invitationId == null || invitationId.trim().isEmpty()) {
            return;
        }
        db.collection(COLLECTION_INVITATIONS)
                .document(invitationId)
                .update("status", Invitation.STATUS_DISMISSED);
    }

    @Override
    public void sendNotification(Notification notification) {
        db.collection(COLLECTION_NOTIFICATIONS).add(notification);
    }

    @Override
    public void respondToInvitation(String invitationId, boolean accept) {
        db.collection(COLLECTION_INVITATIONS).document(invitationId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            Invitation invitation = Invitation.fromDocument(doc);
            String status = accept ? Invitation.STATUS_ACCEPTED : Invitation.STATUS_REJECTED;
            
            db.collection(COLLECTION_INVITATIONS).document(invitationId).update("status", status);

            if (accept) {
                String collection;
                switch (invitation.getType()) {
                    case Invitation.TYPE_MEAL_PLAN: collection = "meal_plans"; break;
                    case Invitation.TYPE_SHOPPING_LIST: collection = "shopping_lists"; break;
                    case Invitation.TYPE_COOKBOOK: collection = "cookbooks"; break;
                    default: return;
                }
                db.collection(collection).document(invitation.getTargetId())
                        .update("collaboratorIds", FieldValue.arrayUnion(invitation.getToUserId()));
            }
            
            // Delete the corresponding notification after response
            db.collection(COLLECTION_NOTIFICATIONS)
                    .whereEqualTo("data.invitationId", invitationId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (DocumentSnapshot nDoc : querySnapshot.getDocuments()) {
                            nDoc.getReference().delete();
                        }
                    });
        });
    }
}
