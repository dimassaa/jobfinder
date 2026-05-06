package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.UserTagsUpdateRequest;
import com.jobapp.server.service.UserService;
import com.jobapp.server.service.RecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class UpdateUserTagsCommand {
    private static final Logger logger = LoggerFactory.getLogger(UpdateUserTagsCommand.class);

    private final UserService userService;
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;
    private final int userId;

    public UpdateUserTagsCommand(UserService userService, RecommendationService recommendationService, ObjectMapper objectMapper, int userId) {
        this.userService = userService;
        this.recommendationService = recommendationService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing UPDATE_USER_TAGS for user: {}", userId);
        try {
            String dataJson = objectMapper.writeValueAsString(msg.getData());
            UserTagsUpdateRequest request = objectMapper.readValue(dataJson, UserTagsUpdateRequest.class);

            List<Integer> tagIds = request.getTagIds();
            userService.updateUserTags(userId, tagIds);

            recommendationService.clearCache(userId);
            logger.info("Cleared recommendation cache for user: {}", userId);

            return new Response("OK", "Tags updated successfully", tagIds);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid tag data for user {}: {}", userId, e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error updating tags for user: {}", userId, e);
            return new Response("ERROR", "Failed to update tags", null);
        }
    }
}