package com.recipebookpro.domain.repository;

import com.recipebookpro.domain.model.Notification;
import java.util.List;
import androidx.lifecycle.LiveData;

public interface NotificationRepository {
    LiveData<List<Notification>> getNotifications(String userId);
    void markAsRead(String notificationId);
    void deleteNotification(Notification notification);
    void sendNotification(Notification notification);
    void respondToInvitation(String invitationId, boolean accept);
}
