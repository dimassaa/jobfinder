package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.VacancyCreateRequest;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.VacancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateVacancyCommand {
    private static final Logger logger = LoggerFactory.getLogger(CreateVacancyCommand.class);

    private final VacancyService vacancyService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final int userId;

    public CreateVacancyCommand(VacancyService vacancyService, UserRepository userRepository,
                           CompanyRepository companyRepository, com.fasterxml.jackson.databind.ObjectMapper objectMapper, int userId) {
        this.vacancyService = vacancyService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing CREATE_VACANCY for user: {}", userId);
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return new Response("ERROR", "User not found", null);
            }

            if (!"HR".equalsIgnoreCase(user.getRole())) {
                return new Response("ERROR", "Only HR can create vacancies", null);
            }

            var company = companyRepository.findByUserId(userId).orElse(null);
            if (company == null) {
                logger.info("No company found for user {}, creating new company", userId);
                var newCompany = new com.jobapp.server.model.Company();
                newCompany.setUserId(userId);
                newCompany.setCompanyName(user.getName() + " Company");
                newCompany.setWebsite("");
                newCompany.setDescription("");
                company = companyRepository.save(newCompany);
                logger.info("Auto-created company with id {} for user {}", company.getId(), userId);
            }

            VacancyCreateRequest request = objectMapper.convertValue(msg.getData(), VacancyCreateRequest.class);
            VacancyDto vacancy = vacancyService.create(company.getId(), request, userId);
            return new Response("OK", "Vacancy created successfully", vacancy);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error creating vacancy for user: {}", userId, e);
            return new Response("ERROR", "Failed to create vacancy", null);
        }
    }
}