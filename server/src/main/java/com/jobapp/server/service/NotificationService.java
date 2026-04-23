package com.jobapp.server.service;

import com.jobapp.server.model.Company;
import com.jobapp.server.model.Notification;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.NotificationRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final VacancyRepository vacancyRepository;
    private final CompanyRepository companyRepository;

    public NotificationService(NotificationRepository notificationRepository,
                              VacancyRepository vacancyRepository,
                              CompanyRepository companyRepository) {
        this.notificationRepository = notificationRepository;
        this.vacancyRepository = vacancyRepository;
        this.companyRepository = companyRepository;
    }

    public void createHrNotification(int vacancyId, String vacancyTitle, int applicantUserId) {
        logger.debug("Creating HR notification for vacancy: {}", vacancyId);

        Optional<Company> companyOpt = companyRepository.findByVacancyId(vacancyId);
        if (companyOpt.isEmpty()) {
            logger.warn("Company not found for vacancy: {}", vacancyId);
            return;
        }

        Company company = companyOpt.get();
        int hrUserId = company.getUserId();

        Notification notification = new Notification();
        notification.setUserId(hrUserId);
        notification.setType("NEW_APPLICATION");
        notification.setTitle("New Application");
        notification.setContent("User has applied for vacancy: " + vacancyTitle);
        notification.setLink("/applications?vacancyId=" + vacancyId);
        notification.setIsRead(false);

        notificationRepository.save(notification);
        logger.info("HR notification created for user: {}", hrUserId);
    }

    public void createApplicantNotification(int applicationId, String newStatus, int applicantUserId) {
        logger.debug("Creating applicant notification for application: {}", applicationId);

        Notification notification = new Notification();
        notification.setUserId(applicantUserId);
        notification.setType("APPLICATION_STATUS");
        notification.setTitle("Application Status Updated");
        notification.setContent("Your application status has been changed to: " + newStatus);
        notification.setLink("/myapplications?applicationId=" + applicationId);
        notification.setIsRead(false);

        notificationRepository.save(notification);
        logger.info("Applicant notification created for user: {}", applicantUserId);
    }

    public void createAdminNotification(String type, String title, String content) {
        logger.debug("Creating admin notification");

        Notification notification = new Notification();
        notification.setUserId(1);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setLink("/admin");
        notification.setIsRead(false);

        notificationRepository.save(notification);
        logger.info("Admin notification created");
    }
}