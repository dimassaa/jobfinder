package com.jobapp.server.handler;

import com.jobapp.common.Response;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.service.FavoriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetFavoritesCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetFavoritesCommand.class);

    private final FavoriteService favoriteService;
    private final int userId;

    public GetFavoritesCommand(FavoriteService favoriteService, int userId) {
        this.favoriteService = favoriteService;
        this.userId = userId;
    }

    public Response execute() {
        logger.info("Executing GET_FAVORITES for user: {}", userId);
        try {
            List<VacancyDto> favorites = favoriteService.getFavorites(userId);
            return new Response("OK", "Favorites retrieved", favorites);
        } catch (Exception e) {
            logger.error("Error getting favorites for user: {}", userId, e);
            return new Response("ERROR", "Failed to retrieve favorites", null);
        }
    }
}