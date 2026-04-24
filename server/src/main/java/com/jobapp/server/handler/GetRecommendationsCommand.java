package com.jobapp.server.handler;

import com.jobapp.common.Response;
import com.jobapp.common.dto.RecommendedVacancyDto;
import com.jobapp.server.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetRecommendationsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetRecommendationsCommand.class);

    private final RecommendationService recommendationService;
    private final int userId;

    public GetRecommendationsCommand(RecommendationService recommendationService, int userId) {
        this.recommendationService = recommendationService;
        this.userId = userId;
    }

    public Response execute() {
        logger.info("Executing GET_RECOMMENDATIONS for user: {}", userId);
        try {
            List<RecommendedVacancyDto> recommendations = recommendationService.getRecommendations(userId);
            return new Response("OK", "Recommendations retrieved", recommendations);
        } catch (Exception e) {
            logger.error("Error getting recommendations for user: {}", userId, e);
            return new Response("ERROR", "Failed to retrieve recommendations", null);
        }
    }
}