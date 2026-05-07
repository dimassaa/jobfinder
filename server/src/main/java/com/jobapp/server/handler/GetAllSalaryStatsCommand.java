package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.SalaryStatsDto;
import com.jobapp.server.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetAllSalaryStatsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetAllSalaryStatsCommand.class);

    private final AnalyticsService analyticsService;
    private final int userId;

    public GetAllSalaryStatsCommand(AnalyticsService analyticsService, int userId) {
        this.analyticsService = analyticsService;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_ALL_SALARY_STATS for user {}", userId);
        try {
            int periodDays = 30;
            if (msg.getData() != null && msg.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) msg.getData();
                if (data.containsKey("periodDays")) {
                    periodDays = ((Number) data.get("periodDays")).intValue();
                }
            }

            SalaryStatsDto stats = analyticsService.getAllSalaryStats(periodDays);
            return new Response("OK", "All salary statistics retrieved", stats);
        } catch (Exception e) {
            logger.error("Error getting all salary stats for user {}", userId, e);
            return new Response("ERROR", "Failed to get all salary statistics: " + e.getMessage(), null);
        }
    }
}