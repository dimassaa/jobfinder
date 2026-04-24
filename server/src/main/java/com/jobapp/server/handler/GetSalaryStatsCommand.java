package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.SalaryStatsDto;
import com.jobapp.server.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetSalaryStatsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetSalaryStatsCommand.class);

    private final AnalyticsService analyticsService;
    private final int userId;

    public GetSalaryStatsCommand(AnalyticsService analyticsService, int userId) {
        this.analyticsService = analyticsService;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_SALARY_STATS for user {}", userId);
        try {
            int periodDays = 30;
            if (msg.getData() != null && msg.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) msg.getData();
                if (data.containsKey("periodDays")) {
                    periodDays = ((Number) data.get("periodDays")).intValue();
                }
            }
            
            SalaryStatsDto stats = analyticsService.getSalaryStatsForUser(userId, periodDays);
            return new Response("OK", "Salary statistics retrieved", stats);
        } catch (Exception e) {
            logger.error("Error getting salary stats for user {}", userId, e);
            return new Response("ERROR", "Failed to get salary statistics: " + e.getMessage(), null);
        }
    }
}