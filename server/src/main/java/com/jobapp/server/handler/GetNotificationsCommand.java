package com.jobapp.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Response;
import com.jobapp.common.dto.NotificationDto;
import com.jobapp.server.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetNotificationsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetNotificationsCommand.class);

    private final NotificationService notificationService;
    private final int userId;
    private final ObjectMapper objectMapper;

    public GetNotificationsCommand(NotificationService notificationService, int userId, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.userId = userId;
        this.objectMapper = objectMapper;
    }

    public Response execute() {
        logger.info("Executing GET_NOTIFICATIONS for user: {}", userId);
        try {
            List<NotificationDto> notifications = notificationService.getUserNotifications(userId, false, 50, 0);
            return new Response("OK", "Notifications retrieved", notifications);
        } catch (Exception e) {
            logger.error("Error retrieving notifications for user: {}", userId, e);
            return new Response("ERROR", "Failed to retrieve notifications", null);
        }
    }
}