package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.service.VacancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetVacancyDetailsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetVacancyDetailsCommand.class);

    private final VacancyService vacancyService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public GetVacancyDetailsCommand(VacancyService vacancyService, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.vacancyService = vacancyService;
        this.objectMapper = objectMapper;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_VACANCY_DETAILS");
        try {
            var data = msg.getData();
            Integer vacancyId = objectMapper.convertValue(data, Integer.class);

            VacancyDto vacancy = vacancyService.getById(vacancyId);
            if (vacancy == null) {
                return new Response("ERROR", "Vacancy not found", null);
            }
            return new Response("OK", "Vacancy details retrieved", vacancy);
        } catch (Exception e) {
            logger.error("Error getting vacancy details", e);
            return new Response("ERROR", "Failed to get vacancy details", null);
        }
    }
}