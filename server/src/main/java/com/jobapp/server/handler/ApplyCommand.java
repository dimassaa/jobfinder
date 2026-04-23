package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ApplicationDto;
import com.jobapp.common.dto.FavoriteRequest;
import com.jobapp.server.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplyCommand {
    private static final Logger logger = LoggerFactory.getLogger(ApplyCommand.class);

    private final ApplicationService applicationService;
    private final ObjectMapper objectMapper;
    private final int userId;

    public ApplyCommand(ApplicationService applicationService, ObjectMapper objectMapper, int userId) {
        this.applicationService = applicationService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing APPLY for user: {}", userId);
        try {
            Integer vacancyId = null;
            if (msg.getData() != null) {
                String dataJson = objectMapper.writeValueAsString(msg.getData());
                FavoriteRequest request = objectMapper.readValue(dataJson, FavoriteRequest.class);
                vacancyId = request.getVacancyId();
            }
            if (vacancyId == null) {
                return new Response("ERROR", "vacancyId is required", null);
            }

            ApplicationDto application = applicationService.apply(userId, vacancyId);
            return new Response("OK", "Application submitted successfully", application);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Bad request: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error applying for vacancy for user: {}", userId, e);
            return new Response("ERROR", "Failed to submit application", null);
        }
    }
}