package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.GeoDistributionDto;
import com.jobapp.server.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetGeoStatsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetGeoStatsCommand.class);

    private final AnalyticsService analyticsService;

    public GetGeoStatsCommand(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_GEO_STATS");
        try {
            int periodDays = 30;
            String type = "VACANCY";
            
            if (msg.getData() != null && msg.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) msg.getData();
                if (data.containsKey("periodDays")) {
                    periodDays = ((Number) data.get("periodDays")).intValue();
                }
                if (data.containsKey("type")) {
                    type = (String) data.get("type");
                }
            }
            
            GeoDistributionDto geoStats;
            if ("APPLICANT".equalsIgnoreCase(type)) {
                geoStats = analyticsService.getGeoDistributionForApplicants(periodDays);
            } else {
                geoStats = analyticsService.getGeoDistributionForVacancies(periodDays);
            }
            
            return new Response("OK", "Geo statistics retrieved", geoStats);
        } catch (Exception e) {
            logger.error("Error getting geo stats", e);
            return new Response("ERROR", "Failed to get geo statistics: " + e.getMessage(), null);
        }
    }
}