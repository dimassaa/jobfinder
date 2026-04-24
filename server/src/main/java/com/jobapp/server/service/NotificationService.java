package com.jobapp.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.dto.NotificationDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Notification;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.NotificationRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static NotificationService instance;

    private final NotificationRepository notificationRepository;
    private final VacancyRepository vacancyRepository;
    private final CompanyRepository companyRepository;
    private final SessionManager sessionManager;

    public static NotificationService getInstance() {
        return instance;
    }

    public static void setInstance(NotificationService notificationService) {
        instance = notificationService;
    }

    public NotificationService(NotificationRepository notificationRepository,
                              VacancyRepository vacancyRepository,
                              CompanyRepository companyRepository,
                              SessionManager sessionManager) {
        this.notificationRepository = notificationRepository;
        this.vacancyRepository = vacancyRepository;
        this.companyRepository = companyRepository;
        this.sessionManager = sessionManager;
        instance = this;
    }

    public void sendToUser(int userId, NotificationDto notificationDto) {
        logger.debug("Sending notification to user: {}", userId);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(notificationDto.getType());
        notification.setTitle(notificationDto.getTitle());
        notification.setContent(notificationDto.getContent());
        notification.setLink(notificationDto.getLink());
        notification.setIsRead(false);

        notificationRepository.save(notification);
        logger.info("Notification saved to DB for user: {}", userId);

        notificationDto.setId(notification.getId());
        notificationDto.setUserId(userId);
        notificationDto.setIsRead(false);
        notificationDto.setCreatedAt(notification.getCreatedAt());

        sendPush(userId, notificationDto);
    }

    public void sendPush(int userId, NotificationDto notification) {
        if (!sessionManager.isOnline(userId)) {
            logger.debug("User {} is offline, push not sent", userId);
            return;
        }

        PrintWriter writer = sessionManager.getSession(userId);
        if (writer == null) {
            logger.warn("No session writer found for user: {}", userId);
            return;
        }

        synchronized (writer) {
            try {
                String json = objectMapper.writeValueAsString(notification);
                String message = "{\"type\":\"NOTIFICATION\",\"data\":" + json + "}";
                writer.println(message);
                writer.flush();
                logger.info("Push notification sent to user: {}", userId);
            } catch (Exception e) {
                logger.error("Error sending push notification to user: {}", userId, e);
            }
        }
    }

    public List<NotificationDto> getUserNotifications(int userId, boolean unreadOnly, int limit, int offset) {
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findUnreadByUserId(userId);
            return notifications.stream().map(this::toDto).collect(Collectors.toList());
        } else {
            notifications = notificationRepository.findAllByUserId(userId, limit, offset);
            return notifications.stream().map(this::toDto).collect(Collectors.toList());
        }
    }

    public void markAsRead(int notificationId, int userId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) {
            logger.warn("Notification not found: {}", notificationId);
            return;
        }

        Notification notification = notificationOpt.get();
        if (notification.getUserId() != userId) {
            logger.warn("User {} attempted to mark notification {} owned by another user", userId, notificationId);
            return;
        }

        notificationRepository.markAsRead(notificationId);
        logger.info("Notification {} marked as read by user {}", notificationId, userId);
    }

    public void deleteNotification(int notificationId, int userId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) {
            logger.warn("Notification not found: {}", notificationId);
            return;
        }

        Notification notification = notificationOpt.get();
        if (notification.getUserId() != userId) {
            logger.warn("User {} attempted to delete notification {} owned by another user", userId, notificationId);
            return;
        }

        notificationRepository.delete(notificationId);
        logger.info("Notification {} deleted by user {}", notificationId, userId);
    }

    private NotificationDto toDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setLink(notification.getLink());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
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