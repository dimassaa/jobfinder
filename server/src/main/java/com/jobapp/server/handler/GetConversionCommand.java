package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ConversionStatsDto;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class GetConversionCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetConversionCommand.class);

    private final AnalyticsService analyticsService;
    private final int userId;
    private final UserRepository userRepository;

    public GetConversionCommand(AnalyticsService analyticsService, int userId, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userId = userId;
        this.userRepository = userRepository;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_CONVERSION for user {}", userId);
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return new Response("ERROR", "User not found", null);
            }
            
            User user = userOpt.get();
            if (!"HR".equalsIgnoreCase(user.getRole())) {
                return new Response("ERROR", "Access denied. Only HR can view conversion stats.", null);
            }
            
            if (msg.getData() == null || !((Map<?, ?>) msg.getData()).containsKey("vacancyId")) {
                return new Response("ERROR", "vacancyId is required", null);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) msg.getData();
            int vacancyId = ((Number) data.get("vacancyId")).intValue();
            
            ConversionStatsDto stats = analyticsService.getVacancyConversion(vacancyId, userId);
            return new Response("OK", "Conversion statistics retrieved", stats);
        } catch (RuntimeException e) {
            logger.error("Error getting conversion stats: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error getting conversion stats", e);
            return new Response("ERROR", "Failed to get conversion statistics: " + e.getMessage(), null);
        }
    }
}