package com.jobapp.server.service;

import com.jobapp.common.dto.ComplaintCreateRequest;
import com.jobapp.server.model.Complaint;
import com.jobapp.server.repository.ComplaintRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplaintService {

    private static final Logger logger = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final VacancyRepository vacancyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ComplaintService(ComplaintRepository complaintRepository,
                       VacancyRepository vacancyRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.complaintRepository = complaintRepository;
        this.vacancyRepository = vacancyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public void create(int userId, ComplaintCreateRequest req) {
        String targetType = req.getTargetType();
        Integer targetId = req.getTargetId();

        if (targetType == null || targetId == null) {
            throw new IllegalArgumentException("targetType and targetId are required");
        }

        boolean targetExists;
        if ("VACANCY".equalsIgnoreCase(targetType)) {
            targetExists = vacancyRepository.findById(targetId).isPresent();
        } else if ("USER".equalsIgnoreCase(targetType)) {
            targetExists = userRepository.findById(targetId).isPresent();
        } else {
            throw new IllegalArgumentException("Invalid targetType: " + targetType);
        }

        if (!targetExists) {
            throw new IllegalArgumentException("Target not found: " + targetType + " with id " + targetId);
        }

        Complaint complaint = new Complaint();
        complaint.setUserId(userId);
        complaint.setTargetType(targetType.toUpperCase());
        complaint.setTargetId(targetId);
        complaint.setReason(req.getReason());
        complaint.setStatus("PENDING");

        complaintRepository.save(complaint);
        logger.info("Complaint created by user {} for {} id {}", userId, targetType, targetId);

        String adminTitle = "New Complaint Submitted";
        String adminContent = String.format("User %d filed a complaint on %s %d: %s",
                userId, targetType, targetId, req.getReason());
        notificationService.createAdminNotification("COMPLAINT", adminTitle, adminContent);
    }
}