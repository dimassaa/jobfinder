package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ApplicationStatusUpdateRequest;
import com.jobapp.server.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateApplicationStatusCommand {
    private static final Logger logger = LoggerFactory.getLogger(UpdateApplicationStatusCommand.class);

    private final ApplicationService applicationService;
    private final ObjectMapper objectMapper;
    private final int hrUserId;

    public UpdateApplicationStatusCommand(ApplicationService applicationService, ObjectMapper objectMapper, int hrUserId) {
        this.applicationService = applicationService;
        this.objectMapper = objectMapper;
        this.hrUserId = hrUserId;
    }

    public Response execute(Message msg) {
        logger.info("Executing UPDATE_APPLICATION_STATUS for HR: {}", hrUserId);
        try {
            ApplicationStatusUpdateRequest request = null;
            if (msg.getData() != null) {
                String dataJson = objectMapper.writeValueAsString(msg.getData());
                request = objectMapper.readValue(dataJson, ApplicationStatusUpdateRequest.class);
            }
            if (request == null || request.getApplicationId() == null || request.getNewStatus() == null) {
                return new Response("ERROR", "applicationId and newStatus are required", null);
            }

            applicationService.updateStatus(request.getApplicationId(), request.getNewStatus(), hrUserId);
            return new Response("OK", "Application status updated", null);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error updating application status for HR: {}", hrUserId, e);
            return new Response("ERROR", "Failed to update application status", null);
        }
    }
}