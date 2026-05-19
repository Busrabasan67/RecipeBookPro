package com.recipebookpro.presentation.ui.notification;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.recipebookpro.data.repository.NotificationRepositoryImpl;
import com.recipebookpro.domain.model.Notification;
import com.recipebookpro.domain.repository.NotificationRepository;
import com.recipebookpro.domain.usecase.DeleteNotificationUseCase;

import java.util.List;

public class NotificationViewModel extends ViewModel {

    private final NotificationRepository repository;
    private final DeleteNotificationUseCase deleteNotificationUseCase;
    private final String currentUserId;

    public NotificationViewModel() {
        this.repository = new NotificationRepositoryImpl();
        this.deleteNotificationUseCase = new DeleteNotificationUseCase(repository);
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public LiveData<List<Notification>> getNotifications() {
        if (currentUserId == null) return null;
        return repository.getNotifications(currentUserId);
    }

    public void markAsRead(String notificationId) {
        repository.markAsRead(notificationId);
    }

    public void respondToInvitation(String invitationId, boolean accept) {
        repository.respondToInvitation(invitationId, accept);
    }

    public void deleteNotification(Notification notification) {
        deleteNotificationUseCase.execute(notification);
    }
}
