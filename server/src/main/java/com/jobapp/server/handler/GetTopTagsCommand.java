package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.TagUsageDto;
import com.jobapp.server.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class GetTopTagsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetTopTagsCommand.class);

    private final AnalyticsService analyticsService;

    public GetTopTagsCommand(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_TOP_TAGS");
        try {
            int periodDays = 30;
            int limit = 10;
            
            if (msg.getData() != null && msg.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) msg.getData();
                if (data.containsKey("periodDays")) {
                    periodDays = ((Number) data.get("periodDays")).intValue();
                }
                if (data.containsKey("limit")) {
                    limit = ((Number) data.get("limit")).intValue();
                }
            }
            
            List<TagUsageDto> topTags = analyticsService.getTopTags(periodDays, limit);
            return new Response("OK", "Top tags retrieved", topTags);
        } catch (Exception e) {
            logger.error("Error getting top tags", e);
            return new Response("ERROR", "Failed to get top tags: " + e.getMessage(), null);
        }
    }
}