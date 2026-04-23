package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.VacancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseVacancyCommand {
    private static final Logger logger = LoggerFactory.getLogger(CloseVacancyCommand.class);

    private final VacancyService vacancyService;
    private final UserRepository userRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final int userId;

    public CloseVacancyCommand(VacancyService vacancyService, UserRepository userRepository,
                              com.fasterxml.jackson.databind.ObjectMapper objectMapper, int userId) {
        this.vacancyService = vacancyService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing CLOSE_VACANCY for user: {}", userId);
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return new Response("ERROR", "User not found", null);
            }

            if (!"HR".equalsIgnoreCase(user.getRole())) {
                return new Response("ERROR", "Only HR can close vacancies", null);
            }

            var data = msg.getData();
            Integer vacancyId = objectMapper.convertValue(data, Integer.class);

            vacancyService.close(vacancyId, userId);
            return new Response("OK", "Vacancy closed successfully", null);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error closing vacancy for user: {}", userId, e);
            return new Response("ERROR", "Failed to close vacancy", null);
        }
    }
}