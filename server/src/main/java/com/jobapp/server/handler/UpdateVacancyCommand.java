package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.VacancyCreateRequest;
import com.jobapp.common.dto.VacancyUpdateRequest;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.VacancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateVacancyCommand {
    private static final Logger logger = LoggerFactory.getLogger(UpdateVacancyCommand.class);

    private final VacancyService vacancyService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final int userId;

    public UpdateVacancyCommand(VacancyService vacancyService, UserRepository userRepository,
                                CompanyRepository companyRepository, com.fasterxml.jackson.databind.ObjectMapper objectMapper, int userId) {
        this.vacancyService = vacancyService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing UPDATE_VACANCY for user: {}", userId);
        try {
            if (msg == null || msg.getData() == null) {
                return new Response("ERROR", "Invalid request data", null);
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return new Response("ERROR", "User not found", null);
            }

            if (!"HR".equalsIgnoreCase(user.getRole())) {
                return new Response("ERROR", "Only HR can update vacancies", null);
            }

            var updateRequest = objectMapper.convertValue(msg.getData(), VacancyUpdateRequest.class);
            if (updateRequest == null || updateRequest.getVacancyId() == 0 || updateRequest.getVacancy() == null) {
                return new Response("ERROR", "Invalid vacancy update request", null);
            }

            vacancyService.update(updateRequest.getVacancyId(), userId, updateRequest.getVacancy());
            return new Response("OK", "Vacancy updated successfully", null);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error updating vacancy for user: {}", userId, e);
            return new Response("ERROR", "Failed to update vacancy", null);
        }
    }
}