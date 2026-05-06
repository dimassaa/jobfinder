package com.jobapp.server.service;

import com.jobapp.common.dto.ApplicationDto;
import com.jobapp.server.model.Application;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.User;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.ApplicationRepository;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);
    private static final int SPAM_THRESHOLD = 10;
    private static final java.util.Set<String> VALID_STATUSES = java.util.Set.of(
            "PENDING", "INVITED", "ACCEPTED", "REJECTED", "WITHDRAWN");

    private final ApplicationRepository applicationRepository;
    private final VacancyRepository vacancyRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              VacancyRepository vacancyRepository,
                              UserRepository userRepository,
                              CompanyRepository companyRepository,
                              NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.vacancyRepository = vacancyRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.notificationService = notificationService;
    }

    public ApplicationDto apply(int userId, int vacancyId) {
        logger.debug("User {} applying for vacancy {}", userId, vacancyId);

        Optional<Vacancy> vacancyOpt = vacancyRepository.findById(vacancyId);
        if (vacancyOpt.isEmpty()) {
            logger.warn("Vacancy not found: {}", vacancyId);
            throw new IllegalArgumentException("Vacancy not found: " + vacancyId);
        }

        Vacancy vacancy = vacancyOpt.get();
        if (!Boolean.TRUE.equals(vacancy.getIsActive())) {
            logger.warn("Vacancy is not active: {}", vacancyId);
            throw new IllegalArgumentException("Vacancy is not active");
        }

        Optional<Application> existing = applicationRepository.findExisting(userId, vacancyId);
        if (existing.isPresent()) {
            logger.warn("User {} already applied for vacancy {}", userId, vacancyId);
            throw new IllegalStateException("You have already applied for this vacancy");
        }

        int recentCount = applicationRepository.countByUserLastMinute(userId);
        if (recentCount >= SPAM_THRESHOLD) {
            logger.warn("Suspicious activity detected: user {} made {} applications in last minute", userId, recentCount);
            throw new IllegalStateException("Too many applications. Please try again later.");
        }

        Application application = new Application(vacancyId, userId);
        application.setStatus("PENDING");
        application = applicationRepository.save(application);

        notificationService.createHrNotification(vacancyId, vacancy.getTitle(), userId);

        Optional<User> userOpt = userRepository.findById(userId);
        String userName = userOpt.map(u -> u.getName()).orElse(null);
        String userSurname = userOpt.map(u -> u.getSurname()).orElse(null);

        return toDto(application, vacancy.getTitle(), userName, userSurname);
    }

    public List<ApplicationDto> getUserApplications(int userId) {
        logger.debug("Getting applications for user: {}", userId);
        List<Application> applications = applicationRepository.findByUserId(userId);

        return applications.stream()
                .map(app -> {
                    Optional<Vacancy> vacancyOpt = vacancyRepository.findById(app.getVacancyId());
                    String vacancyTitle = vacancyOpt.map(Vacancy::getTitle).orElse(null);
                    return toDto(app, vacancyTitle, null, null);
                })
                .collect(Collectors.toList());
    }

    public List<ApplicationDto> getAllApplicationsForHr(int hrUserId) {
        logger.debug("Getting all applications for HR user: {}", hrUserId);

        Optional<Company> companyOpt = companyRepository.findByUserId(hrUserId);
        if (companyOpt.isEmpty()) {
            logger.warn("Company not found for HR user: {}", hrUserId);
            return List.of();
        }

        Company company = companyOpt.get();
        List<Vacancy> vacancies = vacancyRepository.findByCompanyId(company.getId());

        if (vacancies.isEmpty()) {
            return List.of();
        }

        List<Integer> vacancyIds = vacancies.stream()
                .map(Vacancy::getId)
                .collect(Collectors.toList());

        return applicationRepository.findByVacancyIds(vacancyIds).stream()
                .map(app -> {
                    Optional<Vacancy> vacancyOpt = vacancyRepository.findById(app.getVacancyId());
                    String vacancyTitle = vacancyOpt.map(Vacancy::getTitle).orElse("Вакансия");
                    Optional<User> userOpt = userRepository.findById(app.getUserId());
                    String userName = userOpt.map(u -> formatUserName(u.getName(), u.getSurname())).orElse("Соискатель");
                    return toDto(app, vacancyTitle, userName, null);
                })
                .collect(Collectors.toList());
    }

    public List<ApplicationDto> getApplicationsForVacancy(int vacancyId, int hrUserId) {
        logger.debug("Getting applications for vacancy: {} by HR: {}", vacancyId, hrUserId);

        Optional<Vacancy> vacancyOpt = vacancyRepository.findById(vacancyId);
        if (vacancyOpt.isEmpty()) {
            throw new IllegalArgumentException("Vacancy not found: " + vacancyId);
        }

        Vacancy vacancy = vacancyOpt.get();
        if (vacancy.getCompanyId() == null) {
            throw new IllegalArgumentException("Vacancy has no company");
        }

        Optional<Company> companyOpt = companyRepository.findById(vacancy.getCompanyId());
        if (companyOpt.isEmpty()) {
            throw new IllegalStateException("Company not found");
        }

        Company company = companyOpt.get();
        if (company.getUserId() != hrUserId) {
            throw new IllegalArgumentException("You do not have permission to view applications for this vacancy");
        }

        return applicationRepository.findByVacancyIdWithUser(vacancyId).stream()
                .map(app -> toDto(app, vacancy.getTitle(), app.getUserName(), app.getUserSurname()))
                .collect(Collectors.toList());
    }

    public void updateStatus(int applicationId, String newStatus, int hrUserId) {
        logger.debug("Updating application {} status to {} by HR: {}", applicationId, newStatus, hrUserId);

        Optional<Application> appOpt = applicationRepository.findById(applicationId);
        if (appOpt.isEmpty()) {
            throw new IllegalArgumentException("Application not found: " + applicationId);
        }

        Application application = appOpt.get();
        int vacancyId = application.getVacancyId();

        Optional<Vacancy> vacancyOpt = vacancyRepository.findById(vacancyId);
        if (vacancyOpt.isEmpty()) {
            throw new IllegalArgumentException("Vacancy not found");
        }

        Vacancy vacancy = vacancyOpt.get();
        if (vacancy.getCompanyId() == null) {
            throw new IllegalStateException("Vacancy has no company");
        }

        Optional<Company> companyOpt = companyRepository.findById(vacancy.getCompanyId());
        if (companyOpt.isEmpty()) {
            throw new IllegalStateException("Company not found");
        }

        Company company = companyOpt.get();
        if (company.getUserId() != hrUserId) {
            throw new IllegalArgumentException("You do not have permission to update this application");
        }

        if (newStatus == null || !VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status. Must be one of: PENDING, INVITED, ACCEPTED, REJECTED, WITHDRAWN");
        }

        int applicationUserId = application.getUserId();
        applicationRepository.updateStatus(applicationId, newStatus);
        notificationService.createApplicantNotification(applicationId, newStatus, applicationUserId);
    }

    private ApplicationDto toDto(Application application, String vacancyTitle, String userName, String userSurname) {
        return new ApplicationDto(
                application.getId(),
                application.getVacancyId(),
                vacancyTitle,
                application.getUserId(),
                formatUserName(userName, userSurname),
                application.getStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private String formatUserName(String name, String surname) {
        if (name == null && surname == null) {
            return null;
        }
        return (name != null ? name : "") + (surname != null ? " " + surname : "");
    }
}