package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ComplaintCreateRequest;
import com.jobapp.server.service.ComplaintService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateComplaintCommand {
    private static final Logger logger = LoggerFactory.getLogger(CreateComplaintCommand.class);

    private final ComplaintService complaintService;
    private final ObjectMapper objectMapper;
    private final int userId;

    public CreateComplaintCommand(ComplaintService complaintService, ObjectMapper objectMapper, int userId) {
        this.complaintService = complaintService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing CREATE_COMPLAINT for user: {}", userId);
        try {
            ComplaintCreateRequest request = null;
            if (msg.getData() != null) {
                String dataJson = objectMapper.writeValueAsString(msg.getData());
                request = objectMapper.readValue(dataJson, ComplaintCreateRequest.class);
            }
            if (request == null || request.getTargetType() == null || request.getTargetId() == null) {
                return new Response("ERROR", "targetType and targetId are required", null);
            }

            complaintService.create(userId, request);
            return new Response("OK", "Complaint submitted", null);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error creating complaint for user: {}", userId, e);
            return new Response("ERROR", "Failed to submit complaint", null);
        }
    }
}