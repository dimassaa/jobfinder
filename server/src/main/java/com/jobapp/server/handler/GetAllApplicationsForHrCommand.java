package com.jobapp.server.handler;

import com.jobapp.common.Response;
import com.jobapp.common.dto.ApplicationDto;
import com.jobapp.server.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetAllApplicationsForHrCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetAllApplicationsForHrCommand.class);

    private final ApplicationService applicationService;
    private final int hrUserId;

    public GetAllApplicationsForHrCommand(ApplicationService applicationService, int hrUserId) {
        this.applicationService = applicationService;
        this.hrUserId = hrUserId;
    }

    public Response execute() {
        logger.info("Executing GET_ALL_APPLICATIONS_FOR_HR for HR user: {}", hrUserId);
        try {
            List<ApplicationDto> applications = applicationService.getAllApplicationsForHr(hrUserId);
            return new Response("OK", "Applications retrieved", applications);
        } catch (Exception e) {
            logger.error("Error getting applications for HR user: {}", hrUserId, e);
            return new Response("ERROR", "Failed to retrieve applications: " + e.getMessage(), null);
        }
    }
}