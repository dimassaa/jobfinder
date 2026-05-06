package com.jobapp.server.service;

import com.jobapp.common.dto.*;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.User;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.AnalyticsRepository;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private CompanyRepository companyRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsRepository, userRepository, vacancyRepository, companyRepository);
    }

    @Test
    void getSalaryStatsForUser_shouldReturnStatsWithAverage() {
        int userId = 1;
        int periodDays = 30;
        List<ChartDataPointDto> dataPoints = Arrays.asList(
                new ChartDataPointDto("2024-01-01", 3000),
                new ChartDataPointDto("2024-01-02", 55000)
        );

        when(analyticsRepository.getSalaryStatsByUserTags(userId, periodDays)).thenReturn(dataPoints);

        SalaryStatsDto result = analyticsService.getSalaryStatsForUser(userId, periodDays);

        assertNotNull(result);
        assertEquals(2, result.getDataPoints().size());
        assertEquals(29000.0, result.getAverage(), 0.01);
        assertEquals("30 days", result.getPeriod());
        verify(analyticsRepository).getSalaryStatsByUserTags(userId, periodDays);
    }

    @Test
    void getSalaryStatsForUser_shouldReturnZeroAverageWhenNoData() {
        int userId = 1;
        int periodDays = 30;

        when(analyticsRepository.getSalaryStatsByUserTags(userId, periodDays)).thenReturn(List.of());

        SalaryStatsDto result = analyticsService.getSalaryStatsForUser(userId, periodDays);

        assertNotNull(result);
        assertTrue(result.getDataPoints().isEmpty());
        assertEquals(0.0, result.getAverage(), 0.01);
    }

    @Test
    void getTopTags_shouldReturnTagUsageList() {
        int periodDays = 30;
        int limit = 10;
        List<TagUsageDto> topTags = Arrays.asList(
                new TagUsageDto(1, "Java", 100),
                new TagUsageDto(2, "Python", 80)
        );

        when(analyticsRepository.getTopTags(periodDays, limit)).thenReturn(topTags);

        List<TagUsageDto> result = analyticsService.getTopTags(periodDays, limit);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Java", result.get(0).getName());
        assertEquals(100, result.get(0).getCount());
        verify(analyticsRepository).getTopTags(periodDays, limit);
    }

    @Test
    void getGeoDistributionForVacancies_shouldReturnGeoStats() {
        int periodDays = 30;
        List<CityCountDto> cityData = Arrays.asList(
                new CityCountDto("Minsk", 50, 3000.0),
                new CityCountDto("Grodno", 30, 2500.0)
        );

        when(analyticsRepository.getGeoDistribution("VACANCY", periodDays)).thenReturn(cityData);

        GeoDistributionDto result = analyticsService.getGeoDistributionForVacancies(periodDays);

        assertNotNull(result);
        assertEquals(2, result.getDistribution().size());
        verify(analyticsRepository).getGeoDistribution("VACANCY", periodDays);
    }

    @Test
    void getGeoDistributionForApplicants_shouldReturnGeoStats() {
        int periodDays = 30;
        List<CityCountDto> cityData = List.of(
                new CityCountDto("Minsk", 100, 0.0)
        );

        when(analyticsRepository.getGeoDistribution("APPLICANT", periodDays)).thenReturn(cityData);

        GeoDistributionDto result = analyticsService.getGeoDistributionForApplicants(periodDays);

        assertNotNull(result);
        assertEquals(1, result.getDistribution().size());
        verify(analyticsRepository).getGeoDistribution("APPLICANT", periodDays);
    }

    @Test
    void getVacancyConversion_shouldReturnConversionStats() {
        int vacancyId = 1;
        int hrUserId = 10;

        Vacancy vacancy = new Vacancy();
        vacancy.setId(vacancyId);
        vacancy.setTitle("Test Vacancy");

        Company company = new Company();
        company.setId(1);
        company.setUserId(hrUserId);

        ConversionStatsDto expectedStats = new ConversionStatsDto(100, 20, 5, 5.0);

        when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findByVacancyId(vacancyId)).thenReturn(Optional.of(company));
        when(analyticsRepository.getVacancyConversion(vacancyId)).thenReturn(expectedStats);

        ConversionStatsDto result = analyticsService.getVacancyConversion(vacancyId, hrUserId);

        assertNotNull(result);
        assertEquals(100, result.getViews());
        assertEquals(20, result.getResponses());
        assertEquals(5, result.getInvites());
        verify(analyticsRepository).getVacancyConversion(vacancyId);
    }

    @Test
    void getVacancyConversion_shouldThrowWhenVacancyNotFound() {
        int vacancyId = 999;
        int hrUserId = 10;

        when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> analyticsService.getVacancyConversion(vacancyId, hrUserId));
    }

    @Test
    void getVacancyConversion_shouldThrowWhenAccessDenied() {
        int vacancyId = 1;
        int hrUserId = 10;
        int otherHrUserId = 20;

        Vacancy vacancy = new Vacancy();
        vacancy.setId(vacancyId);

        Company company = new Company();
        company.setId(1);
        company.setUserId(otherHrUserId);

        when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findByVacancyId(vacancyId)).thenReturn(Optional.of(company));

        assertThrows(RuntimeException.class, () -> analyticsService.getVacancyConversion(vacancyId, hrUserId));
    }

    @Test
    void getSalaryComparisonForTags_shouldReturnComparisonData() {
        List<Integer> tagIds = Arrays.asList(1, 2, 3);
        List<ChartDataPointDto> comparison = Arrays.asList(
                new ChartDataPointDto("Java", 3000),
                new ChartDataPointDto("Python", 2500)
        );

        when(analyticsRepository.getSalaryStatsByTagIds(tagIds)).thenReturn(comparison);

        List<ChartDataPointDto> result = analyticsService.getSalaryComparisonForTags(tagIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(analyticsRepository).getSalaryStatsByTagIds(tagIds);
    }

    @Test
    void getSalaryComparisonForTags_shouldReturnEmptyForNullTags() {
        List<ChartDataPointDto> result = analyticsService.getSalaryComparisonForTags(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(analyticsRepository, never()).getSalaryStatsByTagIds(any());
    }

    @Test
    void getSalaryComparisonForTags_shouldReturnEmptyForEmptyTags() {
        List<ChartDataPointDto> result = analyticsService.getSalaryComparisonForTags(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(analyticsRepository, never()).getSalaryStatsByTagIds(any());
    }

    @Test
    void getSystemStats_shouldReturnSystemStatistics() {
        SystemStatsDto expectedStats = new SystemStatsDto();
        expectedStats.setTotalUsers(100);
        expectedStats.setTotalVacancies(50);

        when(analyticsRepository.getSystemStats()).thenReturn(expectedStats);

        SystemStatsDto result = analyticsService.getSystemStats();

        assertNotNull(result);
        assertEquals(100, result.getTotalUsers());
        assertEquals(50, result.getTotalVacancies());
        verify(analyticsRepository).getSystemStats();
    }

    @Test
    void getActivityTimeline_shouldReturnTimelineData() {
        int days = 30;
        List<ChartDataPointDto> timeline = Arrays.asList(
                new ChartDataPointDto("2024-01-01", 10),
                new ChartDataPointDto("2024-01-02", 15)
        );

        when(analyticsRepository.getActivityTimeline(days)).thenReturn(timeline);

        List<ChartDataPointDto> result = analyticsService.getActivityTimeline(days);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(analyticsRepository).getActivityTimeline(days);
    }
}