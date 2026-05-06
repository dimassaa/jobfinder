package com.jobapp.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationClientService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationClientService.class);

    private final NetworkService networkService;
    private final ObjectMapper objectMapper;

    public NotificationClientService(NetworkService networkService) {
        this.networkService = networkService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public List<NotificationDto> getNotifications(boolean unreadOnly, int offset, int limit) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("unreadOnly", unreadOnly);
        params.put("offset", offset);
        params.put("limit", limit);

        Message message = new Message("REQUEST", "GET_NOTIFICATIONS", params);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationDto.class));
    }

    public void markAsRead(int notificationId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("notificationId", notificationId);

        Message message = new Message("REQUEST", "MARK_NOTIFICATION_READ", params);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Notification {} marked as read", notificationId);
    }

    public void delete(int notificationId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("notificationId", notificationId);

        Message message = new Message("REQUEST", "DELETE_NOTIFICATION", params);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Notification {} deleted", notificationId);
    }

    public int getUnreadCount() throws Exception {
        Message message = new Message("REQUEST", "GET_UNREAD_COUNT", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return (Integer) response.getData();
    }
}