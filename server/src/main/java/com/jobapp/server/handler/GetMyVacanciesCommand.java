package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.VacancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetMyVacanciesCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetMyVacanciesCommand.class);

    private final VacancyService vacancyService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final int userId;

    public GetMyVacanciesCommand(VacancyService vacancyService, UserRepository userRepository,
                                CompanyRepository companyRepository, int userId) {
        this.vacancyService = vacancyService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing GET_MY_VACANCIES for user: {}", userId);
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return new Response("ERROR", "User not found", null);
            }

            if (!"HR".equalsIgnoreCase(user.getRole())) {
                return new Response("ERROR", "Only HR can view their vacancies", null);
            }

            var company = companyRepository.findByUserId(userId).orElse(null);
            if (company == null) {
                return new Response("ERROR", "Company not found for this user", null);
            }

            List<VacancyDto> vacancies = vacancyService.getByCompany(company.getId());
            return new Response("OK", "Found " + vacancies.size() + " vacancies", vacancies);
        } catch (Exception e) {
            logger.error("Error getting vacancies for user: {}", userId, e);
            return new Response("ERROR", "Failed to get vacancies", null);
        }
    }
}