package com.jobapp.server.service;

import com.jobapp.common.dto.ComplaintCreateRequest;
import com.jobapp.server.model.Complaint;
import com.jobapp.server.model.User;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.ComplaintRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private ComplaintService complaintService;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintService(
                complaintRepository,
                vacancyRepository,
                userRepository,
                notificationService
        );
    }

    @Test
    void create_shouldSaveComplaintForVacancy() {
        int userId = 1;
        int vacancyId = 10;

        Vacancy vacancy = new Vacancy();
        vacancy.setId(vacancyId);
        vacancy.setTitle("Java Developer");

        ComplaintCreateRequest req = new ComplaintCreateRequest("VACANCY", vacancyId, "Spam vacancy");

        when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancy));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint c = invocation.getArgument(0);
            c.setId(100);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        complaintService.create(userId, req);

        verify(complaintRepository).save(any(Complaint.class));
        verify(notificationService).createAdminNotification(
                eq("COMPLAINT"),
                eq("New Complaint Submitted"),
                contains("User 1 filed a complaint on VACANCY 10")
        );
    }

    @Test
    void create_shouldSaveComplaintForUser() {
        int userId = 1;
        int targetUserId = 5;

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setName("John");

        ComplaintCreateRequest req = new ComplaintCreateRequest("USER", targetUserId, "Inappropriate behavior");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint c = invocation.getArgument(0);
            c.setId(100);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        complaintService.create(userId, req);

        verify(complaintRepository).save(any(Complaint.class));
        verify(notificationService).createAdminNotification(
                eq("COMPLAINT"),
                eq("New Complaint Submitted"),
                contains("User 1 filed a complaint on USER 5")
        );
    }

    @Test
    void create_shouldThrowExceptionForNonExistentVacancy() {
        int userId = 1;
        int vacancyId = 999;

        ComplaintCreateRequest req = new ComplaintCreateRequest("VACANCY", vacancyId, "Test reason");

        when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                complaintService.create(userId, req)
        );

        verify(complaintRepository, never()).save(any(Complaint.class));
    }

    @Test
    void create_shouldThrowExceptionForNonExistentUser() {
        int userId = 1;
        int targetUserId = 999;

        ComplaintCreateRequest req = new ComplaintCreateRequest("USER", targetUserId, "Test reason");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                complaintService.create(userId, req)
        );

        verify(complaintRepository, never()).save(any(Complaint.class));
    }

    @Test
    void create_shouldThrowExceptionForInvalidTargetType() {
        int userId = 1;

        ComplaintCreateRequest req = new ComplaintCreateRequest("INVALID", 10, "Test reason");

        assertThrows(IllegalArgumentException.class, () ->
                complaintService.create(userId, req)
        );

        verify(complaintRepository, never()).save(any(Complaint.class));
    }
}