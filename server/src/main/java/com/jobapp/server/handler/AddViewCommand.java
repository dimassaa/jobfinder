package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.FavoriteRequest;
import com.jobapp.server.service.ViewHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddViewCommand {
    private static final Logger logger = LoggerFactory.getLogger(AddViewCommand.class);

    private final ViewHistoryService viewHistoryService;
    private final ObjectMapper objectMapper;
    private final int userId;

    public AddViewCommand(ViewHistoryService viewHistoryService, ObjectMapper objectMapper, int userId) {
        this.viewHistoryService = viewHistoryService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing ADD_VIEW for user: {}", userId);
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

            viewHistoryService.addView(userId, vacancyId);
            return new Response("OK", "View recorded", null);
        } catch (Exception e) {
            logger.error("Error recording view for user: {}", userId, e);
            return new Response("ERROR", "Failed to record view", null);
        }
    }
}