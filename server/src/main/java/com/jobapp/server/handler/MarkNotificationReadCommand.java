package com.jobapp.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkNotificationReadCommand {
    private static final Logger logger = LoggerFactory.getLogger(MarkNotificationReadCommand.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final int userId;

    public MarkNotificationReadCommand(NotificationService notificationService, ObjectMapper objectMapper, int userId) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message message) {
        logger.info("Executing MARK_NOTIFICATION_READ for user: {}", userId);
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
            notificationService.markAsRead(notificationId, userId);
            return new Response("OK", "Notification marked as read", null);
        } catch (Exception e) {
            logger.error("Error marking notification as read for user: {}", userId, e);
            return new Response("ERROR", "Failed to mark notification as read", null);
        }
    }
}