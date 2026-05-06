package com.recipebookpro.service;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.recipebookpro.util.NotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            NotificationHelper.showNotification(this, title, body);
        } else if (!remoteMessage.getData().isEmpty()) {
            // Handle data payload
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            if (title != null && message != null) {
                NotificationHelper.showNotification(this, title, message);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // You can upload this token to your server/Firestore to send targeted notifications
    }
}
