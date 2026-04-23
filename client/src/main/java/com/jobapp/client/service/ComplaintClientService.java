package com.jobapp.client.service;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ComplaintCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplaintClientService {

    private static final Logger logger = LoggerFactory.getLogger(ComplaintClientService.class);

    private final NetworkService networkService;

    public ComplaintClientService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void create(String targetType, int targetId, String reason) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        ComplaintCreateRequest request = new ComplaintCreateRequest();
        request.setTargetType(targetType);
        request.setTargetId(targetId);
        request.setReason(reason);

        Message message = new Message("REQUEST", "CREATE_COMPLAINT", request);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Complaint created for {} with id {}", targetType, targetId);
    }
}