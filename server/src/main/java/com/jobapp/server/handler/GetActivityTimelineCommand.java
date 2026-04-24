package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ChartDataPointDto;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GetActivityTimelineCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetActivityTimelineCommand.class);

    private final AnalyticsService analyticsService;
    private final int userId;
    private final UserRepository userRepository;

    public GetActivityTimelineCommand(AnalyticsService analyticsService, int userId, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userId = userId;
        this.userRepository = userRepository;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_ACTIVITY_TIMELINE for user {}", userId);
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return new Response("ERROR", "User not found", null);
            }
            
            User user = userOpt.get();
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return new Response("ERROR", "Access denied. Only admin can view activity timeline.", null);
            }
            
            int days = 30;
            if (msg.getData() != null && msg.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) msg.getData();
                if (data.containsKey("days")) {
                    days = ((Number) data.get("days")).intValue();
                }
            }
            
            List<ChartDataPointDto> timeline = analyticsService.getActivityTimeline(days);
            return new Response("OK", "Activity timeline retrieved", timeline);
        } catch (Exception e) {
            logger.error("Error getting activity timeline", e);
            return new Response("ERROR", "Failed to get activity timeline: " + e.getMessage(), null);
        }
    }
}