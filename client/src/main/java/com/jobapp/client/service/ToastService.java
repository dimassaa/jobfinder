package com.jobapp.client.service;

import com.jobapp.common.dto.NotificationDto;
import javafx.application.Platform;
import org.controlsfx.control.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToastService {

    private static final Logger logger = LoggerFactory.getLogger(ToastService.class);

    public static void showNotification(NotificationDto notification) {
        if (notification == null) {
            logger.warn("Received null notification");
            return;
        }

        logger.info("Showing toast notification: {}", notification.getTitle());

        Platform.runLater(() -> {
            Notifications notificationPane = Notifications.create()
                    .title(notification.getTitle() != null ? notification.getTitle() : "Уведомление")
                    .text(notification.getContent() != null ? notification.getContent() : "")
                    .hideAfter(javafx.util.Duration.seconds(5));

            String type = notification.getType();
            if ("NEW_APPLICATION".equals(type) || "APPLICATION_STATUS_CHANGED".equals(type)) {
                notificationPane.showInformation();
            } else if ("PROFILE_REMINDER".equals(type)) {
                notificationPane.showWarning();
            } else if ("VACANCY_MATCH".equals(type)) {
                notificationPane.showInformation();
            } else {
                notificationPane.showInformation();
            }

            notificationPane.show();
            logger.info("Toast notification displayed: {}", notification.getTitle());
        });
    }
}