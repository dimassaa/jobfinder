package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.common.dto.VacancyFilterDto;
import com.jobapp.server.service.VacancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SearchVacanciesCommand {
    private static final Logger logger = LoggerFactory.getLogger(SearchVacanciesCommand.class);

    private final VacancyService vacancyService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public SearchVacanciesCommand(VacancyService vacancyService, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.vacancyService = vacancyService;
        this.objectMapper = objectMapper;
    }

    public Response execute(Message msg) {
        logger.info("Executing SEARCH_VACANCIES");
        try {
            VacancyFilterDto filter = objectMapper.convertValue(msg.getData(), VacancyFilterDto.class);
            List<VacancyDto> vacancies = vacancyService.search(filter);
            return new Response("OK", "Found " + vacancies.size() + " vacancies", vacancies);
        } catch (Exception e) {
            logger.error("Error searching vacancies", e);
            return new Response("ERROR", "Failed to search vacancies", null);
        }
    }
}