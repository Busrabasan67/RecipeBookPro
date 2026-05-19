package com.recipebookpro.domain.usecase;

import com.recipebookpro.domain.model.Notification;
import com.recipebookpro.domain.repository.NotificationRepository;

public class DeleteNotificationUseCase {

    private final NotificationRepository notificationRepository;

    public DeleteNotificationUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void execute(Notification notification) {
        if (notification == null || notification.getId() == null || notification.getId().trim().isEmpty()) {
            return;
        }
        notificationRepository.deleteNotification(notification);
    }
}
