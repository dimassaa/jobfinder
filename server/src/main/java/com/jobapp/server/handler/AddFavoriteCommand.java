package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.FavoriteRequest;
import com.jobapp.server.service.FavoriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddFavoriteCommand {
    private static final Logger logger = LoggerFactory.getLogger(AddFavoriteCommand.class);

    private final FavoriteService favoriteService;
    private final ObjectMapper objectMapper;
    private final int userId;

    public AddFavoriteCommand(FavoriteService favoriteService, ObjectMapper objectMapper, int userId) {
        this.favoriteService = favoriteService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing ADD_FAVORITE for user: {}", userId);
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

            favoriteService.add(userId, vacancyId);
            return new Response("OK", "Vacancy added to favorites", null);
        } catch (Exception e) {
            logger.error("Error adding to favorites for user: {}", userId, e);
            return new Response("ERROR", "Failed to add to favorites", null);
        }
    }
}