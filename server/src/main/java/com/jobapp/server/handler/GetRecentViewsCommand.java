package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.service.ViewHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetRecentViewsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetRecentViewsCommand.class);

    private final ViewHistoryService viewHistoryService;
    private final int userId;

    public GetRecentViewsCommand(ViewHistoryService viewHistoryService, int userId) {
        this.viewHistoryService = viewHistoryService;
        this.userId = userId;
    }

    public Response execute() {
        logger.info("Executing GET_RECENT_VIEWS for user: {}", userId);
        try {
            List<VacancyDto> views = viewHistoryService.getRecentViews(userId);
            return new Response("OK", "Views retrieved", views);
        } catch (Exception e) {
            logger.error("Error getting recent views for user: {}", userId, e);
            return new Response("ERROR", "Failed to retrieve views", null);
        }
    }
}