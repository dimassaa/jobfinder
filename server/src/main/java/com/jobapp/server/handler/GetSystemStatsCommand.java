package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.SystemStatsDto;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GetSystemStatsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetSystemStatsCommand.class);

    private final AnalyticsService analyticsService;
    private final int userId;
    private final UserRepository userRepository;

    public GetSystemStatsCommand(AnalyticsService analyticsService, int userId, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userId = userId;
        this.userRepository = userRepository;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_SYSTEM_STATS for user {}", userId);
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return new Response("ERROR", "User not found", null);
            }
            
            User user = userOpt.get();
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return new Response("ERROR", "Access denied. Only admin can view system stats.", null);
            }
            
            SystemStatsDto stats = analyticsService.getSystemStats();
            return new Response("OK", "System statistics retrieved", stats);
        } catch (Exception e) {
            logger.error("Error getting system stats", e);
            return new Response("ERROR", "Failed to get system statistics: " + e.getMessage(), null);
        }
    }
}