package com.jobapp.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteNotificationCommand {
    private static final Logger logger = LoggerFactory.getLogger(DeleteNotificationCommand.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final int userId;

    public DeleteNotificationCommand(NotificationService notificationService, ObjectMapper objectMapper, int userId) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message message) {
        logger.info("Executing DELETE_NOTIFICATION for user: {}", userId);
        try {
            Integer notificationId = null;
            if (message.getData() != null) {
                String dataJson = objectMapper.writeValueAsString(message.getData());
                java.util.Map<String, Object> data = objectMapper.readValue(dataJson, java.util.Map.class);
                Object idObj = data.get("notificationId");
                if (idObj instanceof Number) {
                    notificationId = ((Number) idObj).intValue();
                }
            }
            if (notificationId == null) {
                return new Response("ERROR", "notificationId is required", null);
            }
            notificationService.deleteNotification(notificationId, userId);
            return new Response("OK", "Notification deleted", null);
        } catch (Exception e) {
            logger.error("Error deleting notification for user: {}", userId, e);
            return new Response("ERROR", "Failed to delete notification", null);
        }
    }
}