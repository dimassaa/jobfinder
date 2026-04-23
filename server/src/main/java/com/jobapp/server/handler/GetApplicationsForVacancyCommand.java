package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ApplicationDto;
import com.jobapp.server.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetApplicationsForVacancyCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetApplicationsForVacancyCommand.class);

    private final ApplicationService applicationService;
    private final ObjectMapper objectMapper;
    private final int hrUserId;

    public GetApplicationsForVacancyCommand(ApplicationService applicationService, ObjectMapper objectMapper, int hrUserId) {
        this.applicationService = applicationService;
        this.objectMapper = objectMapper;
        this.hrUserId = hrUserId;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_APPLICATIONS_FOR_VACANCY for HR: {}", hrUserId);
        try {
            Integer vacancyId = null;
            if (msg.getData() != null) {
                String dataJson = objectMapper.writeValueAsString(msg.getData());
                java.util.Map<String, Object> map = objectMapper.readValue(dataJson, java.util.Map.class);
                Object vacancyIdObj = map.get("vacancyId");
                if (vacancyIdObj instanceof Integer) {
                    vacancyId = (Integer) vacancyIdObj;
                } else if (vacancyIdObj instanceof Number) {
                    vacancyId = ((Number) vacancyIdObj).intValue();
                }
            }
            if (vacancyId == null) {
                return new Response("ERROR", "vacancyId is required", null);
            }

            List<ApplicationDto> applications = applicationService.getApplicationsForVacancy(vacancyId, hrUserId);
            return new Response("OK", "Applications retrieved", applications);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error getting applications for vacancy for HR: {}", hrUserId, e);
            return new Response("ERROR", "Failed to retrieve applications", null);
        }
    }
}