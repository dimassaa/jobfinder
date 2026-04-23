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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private NotificationService notificationService;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                applicationRepository,
                vacancyRepository,
                userRepository,
                companyRepository,
                notificationService
        );
    }

    @Test
    void apply_shouldCreateApplicationForActiveVacancy() {
        User user = new User();
        user.setId(1);
        user.setName("John");
        user.setSurname("Doe");

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setTitle("Java Developer");
        vacancy.setIsActive(true);

        Application savedApplication = new Application(10, 1);
        savedApplication.setId(100);
        savedApplication.setStatus("PENDING");
        savedApplication.setCreatedAt(LocalDateTime.now());

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(applicationRepository.findExisting(1, 10)).thenReturn(Optional.empty());
        when(applicationRepository.countByUserLastMinute(1)).thenReturn(0);
        when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ApplicationDto result = applicationService.apply(1, 10);

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("Java Developer", result.getVacancyTitle());

        verify(notificationService).createHrNotification(eq(10), eq("Java Developer"), eq(1));
    }

    @Test
    void apply_shouldThrowExceptionForInactiveVacancy() {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setIsActive(false);

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));

        assertThrows(IllegalArgumentException.class, () ->
                applicationService.apply(1, 10)
        );
    }

    @Test
    void apply_shouldThrowExceptionForDuplicateApplication() {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setIsActive(true);

        Application existingApplication = new Application(10, 1);

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(applicationRepository.findExisting(1, 10)).thenReturn(Optional.of(existingApplication));

        assertThrows(IllegalStateException.class, () ->
                applicationService.apply(1, 10)
        );
    }

    @Test
    void apply_shouldThrowExceptionForSpam() {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setIsActive(true);

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(applicationRepository.findExisting(1, 10)).thenReturn(Optional.empty());
        when(applicationRepository.countByUserLastMinute(1)).thenReturn(10);

        assertThrows(IllegalStateException.class, () ->
                applicationService.apply(1, 10)
        );
    }

    @Test
    void getUserApplications_shouldReturnApplicationsForUser() {
        Application app1 = new Application(10, 1);
        app1.setId(100);
        app1.setStatus("PENDING");
        app1.setCreatedAt(LocalDateTime.now());

        Application app2 = new Application(20, 1);
        app2.setId(101);
        app2.setStatus("INVITED");
        app2.setCreatedAt(LocalDateTime.now());

        Vacancy vacancy1 = new Vacancy();
        vacancy1.setId(10);
        vacancy1.setTitle("Java Developer");

        Vacancy vacancy2 = new Vacancy();
        vacancy2.setId(20);
        vacancy2.setTitle("Python Developer");

        when(applicationRepository.findByUserId(1)).thenReturn(Arrays.asList(app1, app2));
        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy1));
        when(vacancyRepository.findById(20)).thenReturn(Optional.of(vacancy2));

        List<ApplicationDto> result = applicationService.getUserApplications(1);

        assertEquals(2, result.size());
        assertEquals("Java Developer", result.get(0).getVacancyTitle());
    }

    @Test
    void getApplicationsForVacancy_shouldReturnApplicationsForHr() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);
        vacancy.setTitle("Java Developer");
        vacancy.setIsActive(true);

        Application app = new Application(10, 1);
        app.setId(100);
        app.setUserId(1);
        app.setStatus("PENDING");
        app.setUserName("John");
        app.setUserSurname("Doe");

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(applicationRepository.findByVacancyIdWithUser(10)).thenReturn(Collections.singletonList(app));

        List<ApplicationDto> result = applicationService.getApplicationsForVacancy(10, 100);

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getUserName());
    }

    @Test
    void getApplicationsForVacancy_shouldThrowExceptionForNonOwner() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);
        vacancy.setTitle("Java Developer");

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));

        assertThrows(IllegalArgumentException.class, () ->
                applicationService.getApplicationsForVacancy(10, 999)
        );
    }

    @Test
    void updateStatus_shouldUpdateAndNotifyApplicant() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);
        vacancy.setTitle("Java Developer");

        Application application = new Application(10, 1);
        application.setId(100);
        application.setUserId(1);
        application.setStatus("PENDING");

        when(applicationRepository.findById(100)).thenReturn(Optional.of(application));
        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));

        applicationService.updateStatus(100, "INVITED", 100);

        verify(applicationRepository).updateStatus(100, "INVITED");
        verify(notificationService).createApplicantNotification(eq(100), eq("INVITED"), eq(1));
    }

    @Test
    void updateStatus_shouldThrowExceptionForNonOwner() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);

        Application application = new Application(10, 1);
        application.setId(100);
        application.setUserId(1);
        application.setStatus("PENDING");

        when(applicationRepository.findById(100)).thenReturn(Optional.of(application));
        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));

        assertThrows(IllegalArgumentException.class, () ->
                applicationService.updateStatus(100, "INVITED", 999)
        );
    }
}