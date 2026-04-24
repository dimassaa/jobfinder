package com.jobapp.server.handler;

import com.jobapp.common.Response;
import com.jobapp.server.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetUnreadCountCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetUnreadCountCommand.class);

    private final NotificationService notificationService;
    private final int userId;

    public GetUnreadCountCommand(NotificationService notificationService, int userId) {
        this.notificationService = notificationService;
        this.userId = userId;
    }

    public Response execute() {
        logger.info("Executing GET_UNREAD_COUNT for user: {}", userId);
        try {
            int count = notificationService.getUnreadCount(userId);
            return new Response("OK", "Unread count retrieved", count);
        } catch (Exception e) {
            logger.error("Error getting unread count for user: {}", userId, e);
            return new Response("ERROR", "Failed to get unread count", null);
        }
    }
}