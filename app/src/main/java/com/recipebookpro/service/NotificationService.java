package com.recipebookpro.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.recipebookpro.domain.model.Notification;
import com.recipebookpro.util.LocaleUtils;
import com.recipebookpro.util.NotificationHelper;

import java.util.HashSet;
import java.util.Set;

public class NotificationService extends Service {

    private ListenerRegistration listenerRegistration;
    private final Set<String> alertedNotificationIds = new HashSet<>();
    private long serviceStartTime;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceStartTime = System.currentTimeMillis();
        startObserving();
        return START_STICKY;
    }

    private void startObserving() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            stopSelf();
            return;
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("recipientId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Notification n = Notification.fromDocument(doc);
                        if (n == null || n.isRead()) continue;

                        // Only alert if:
                        // 1. It's newer than when the service started
                        // 2. We haven't alerted it in this session
                        if (n.getTimestamp() > serviceStartTime && !alertedNotificationIds.contains(n.getId())) {
                            boolean isTr = LocaleUtils.isTurkish(this);
                            NotificationHelper.showNotification(
                                    this,
                                    isTr ? n.getTitleTr() : n.getTitle(),
                                    isTr ? n.getMessageTr() : n.getMessage()
                            );
                            alertedNotificationIds.add(n.getId());
                        }
                    }
                    
                    // Also check invitations
                    checkInvitations(uid);
                });
    }

    private void checkInvitations(String uid) {
        FirebaseFirestore.getInstance()
                .collection("invitations")
                .whereEqualTo("toUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        long createdAt = doc.getLong("createdAt") != null ? doc.getLong("createdAt") : 0;
                        String id = doc.getId();
                        
                        if (createdAt > serviceStartTime && !alertedNotificationIds.contains("inv_" + id)) {
                            String fromName = doc.getString("fromUserName");
                            String targetName = doc.getString("targetName");
                            
                            boolean isTr = LocaleUtils.isTurkish(this);
                            String title = isTr ? "Yeni Davet" : "New Invitation";
                            String msg = isTr ? 
                                    fromName + " sizi " + targetName + " için davet etti" :
                                    fromName + " invited you to " + targetName;
                                    
                            NotificationHelper.showNotification(this, title, msg);
                            alertedNotificationIds.add("inv_" + id);
                        }
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
