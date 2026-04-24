package com.jobapp.server.logging;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.Level;
import com.jobapp.common.dto.NotificationDto;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NotificationAppender extends AppenderBase<ILoggingEvent> {

    private static final Logger logger = LoggerFactory.getLogger(NotificationAppender.class);
    private NotificationService notificationService;
    private UserRepository userRepository;

    public NotificationAppender() {
        try {
            this.userRepository = new UserRepository();
        } catch (Exception e) {
            logger.error("Failed to initialize UserRepository", e);
        }
    }

    @Override
    public void start() {
        if (userRepository == null) {
            try {
                userRepository = new UserRepository();
            } catch (Exception e) {
                logger.error("Failed to initialize UserRepository in start()", e);
                return;
            }
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            notifyAdmins(event);
        }
    }

    private void notifyAdmins(ILoggingEvent event) {
        if (notificationService == null) {
            notificationService = NotificationService.getInstance();
        }

        if (notificationService == null || userRepository == null) {
            logger.warn("Services not available, skipping admin notification");
            return;
        }

        try {
            List<User> admins = userRepository.findAllAdmins();

            String loggerName = event.getLoggerName() != null ? event.getLoggerName() : "Unknown";
            String title = "Server Error: " + loggerName;
            String content = String.format("ERROR: %s | Message: %s",
                    event.getLevel(),
                    event.getMessage());

            if (event.getThrowableProxy() != null) {
                content += " | Exception: " + event.getThrowableProxy().getClassName();
            }

            for (User admin : admins) {
                try {
                    NotificationDto notification = new NotificationDto();
                    notification.setType("SERVER_ERROR");
                    notification.setTitle(title);
                    notification.setContent(content);

                    notificationService.sendToUser(admin.getId(), notification);
                    logger.info("Admin notification sent to user {}", admin.getId());
                } catch (Exception e) {
                    logger.error("Failed to notify admin {}", admin.getId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in notifyAdmins", e);
        }
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}