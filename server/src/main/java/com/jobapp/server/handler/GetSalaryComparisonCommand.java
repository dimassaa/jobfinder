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

public class GetSalaryComparisonCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetSalaryComparisonCommand.class);

    private final AnalyticsService analyticsService;
    private final int userId;
    private final UserRepository userRepository;

    public GetSalaryComparisonCommand(AnalyticsService analyticsService, int userId, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userId = userId;
        this.userRepository = userRepository;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_SALARY_COMPARISON for user {}", userId);
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return new Response("ERROR", "User not found", null);
            }
            
            User user = userOpt.get();
            if (!"HR".equalsIgnoreCase(user.getRole())) {
                return new Response("ERROR", "Access denied. Only HR can view salary comparison.", null);
            }
            
            if (msg.getData() == null || !((Map<?, ?>) msg.getData()).containsKey("tagIds")) {
                return new Response("ERROR", "tagIds is required", null);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) msg.getData();
            @SuppressWarnings("unchecked")
            List<Integer> tagIds = (List<Integer>) data.get("tagIds");
            
            if (tagIds == null || tagIds.isEmpty()) {
                return new Response("OK", "No tags provided", List.of());
            }
            
            List<ChartDataPointDto> comparison = analyticsService.getSalaryComparisonForTags(tagIds);
            return new Response("OK", "Salary comparison retrieved", comparison);
        } catch (Exception e) {
            logger.error("Error getting salary comparison", e);
            return new Response("ERROR", "Failed to get salary comparison: " + e.getMessage(), null);
        }
    }
}