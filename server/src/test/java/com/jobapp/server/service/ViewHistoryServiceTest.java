package com.jobapp.server.service;

import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.ViewHistoryRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewHistoryServiceTest {

    @Mock
    private ViewHistoryRepository viewHistoryRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private TagRepository tagRepository;

    private ViewHistoryService viewHistoryService;

    @BeforeEach
    void setUp() {
        viewHistoryService = new ViewHistoryService(viewHistoryRepository, companyRepository, tagRepository);
    }

    @Test
    void addView_shouldCallRepository() {
        viewHistoryService.addView(1, 10);
        verify(viewHistoryRepository).add(1, 10);
    }

    @Test
    void getRecentViews_shouldReturnVacancyDtos() {
        Vacancy vacancy1 = new Vacancy();
        vacancy1.setId(10);
        vacancy1.setCompanyId(1);
        vacancy1.setTitle("Java Developer");
        vacancy1.setDescription("Backend developer");
        vacancy1.setCity("Moscow");
        vacancy1.setSalaryMin(100000);
        vacancy1.setSalaryMax(200000);
        vacancy1.setEmploymentType("FULL_TIME");
        vacancy1.setIsActive(true);
        vacancy1.setCreatedAt(LocalDateTime.now());

        Vacancy vacancy2 = new Vacancy();
        vacancy2.setId(20);
        vacancy2.setCompanyId(2);
        vacancy2.setTitle("Python Developer");
        vacancy2.setDescription("Data scientist");
        vacancy2.setCity("Saint-Petersburg");
        vacancy2.setIsActive(true);

        Company company1 = new Company();
        company1.setId(1);
        company1.setCompanyName("TechCorp");

        Company company2 = new Company();
        company2.setId(2);
        company2.setCompanyName("DataInc");

        when(viewHistoryRepository.getRecentViews(1, 20)).thenReturn(Arrays.asList(vacancy1, vacancy2));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company1));
        when(companyRepository.findById(2)).thenReturn(Optional.of(company2));
        when(tagRepository.findByVacancyId(10)).thenReturn(Collections.emptyList());
        when(tagRepository.findByVacancyId(20)).thenReturn(Collections.emptyList());

        List<VacancyDto> result = viewHistoryService.getRecentViews(1);

        assertEquals(2, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
        assertEquals("TechCorp", result.get(0).getCompanyName());
    }

    @Test
    void getRecentViews_withLimit_shouldUseCustomLimit() {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);
        vacancy.setTitle("Java Developer");
        vacancy.setIsActive(true);

        Company company = new Company();
        company.setId(1);
        company.setCompanyName("TechCorp");

        when(viewHistoryRepository.getRecentViews(1, 5)).thenReturn(Collections.singletonList(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(tagRepository.findByVacancyId(10)).thenReturn(Collections.emptyList());

        List<VacancyDto> result = viewHistoryService.getRecentViews(1, 5);

        assertEquals(1, result.size());
        verify(viewHistoryRepository).getRecentViews(1, 5);
    }

    @Test
    void getRecentViews_shouldReturnEmptyListWhenNoHistory() {
        when(viewHistoryRepository.getRecentViews(1, 20)).thenReturn(Collections.emptyList());

        List<VacancyDto> result = viewHistoryService.getRecentViews(1);

        assertTrue(result.isEmpty());
    }
}