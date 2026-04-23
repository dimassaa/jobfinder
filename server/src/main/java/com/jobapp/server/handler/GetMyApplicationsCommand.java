package com.jobapp.server.handler;

import com.jobapp.common.Response;
import com.jobapp.common.dto.ApplicationDto;
import com.jobapp.server.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetMyApplicationsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetMyApplicationsCommand.class);

    private final ApplicationService applicationService;
    private final int userId;

    public GetMyApplicationsCommand(ApplicationService applicationService, int userId) {
        this.applicationService = applicationService;
        this.userId = userId;
    }

    public Response execute() {
        logger.info("Executing GET_MY_APPLICATIONS for user: {}", userId);
        try {
            List<ApplicationDto> applications = applicationService.getUserApplications(userId);
            return new Response("OK", "Applications retrieved", applications);
        } catch (Exception e) {
            logger.error("Error getting applications for user: {}", userId, e);
            return new Response("ERROR", "Failed to retrieve applications", null);
        }
    }
}