package com.jobapp.server.service;

import com.jobapp.common.dto.*;
import com.jobapp.server.repository.AnalyticsRepository;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;
    private final VacancyRepository vacancyRepository;
    private final CompanyRepository companyRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository,
                           UserRepository userRepository,
                           VacancyRepository vacancyRepository,
                           CompanyRepository companyRepository) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
        this.vacancyRepository = vacancyRepository;
        this.companyRepository = companyRepository;
    }

    public SalaryStatsDto getSalaryStatsForUser(int userId, int periodDays) {
        logger.info("Getting salary stats for user: {}, period: {} days", userId, periodDays);
        List<ChartDataPointDto> dataPoints = analyticsRepository.getSalaryStatsByUserTags(userId, periodDays);
        
        double average = dataPoints.stream()
                .mapToDouble(ChartDataPointDto::getValue)
                .average()
                .orElse(0.0);
        
        String period = periodDays + " days";
        return new SalaryStatsDto(dataPoints, period, average);
    }

    public List<TagUsageDto> getTopTags(int periodDays, int limit) {
        logger.info("Getting top tags, period: {} days, limit: {}", periodDays, limit);
        return analyticsRepository.getTopTags(periodDays, limit);
    }

    public GeoDistributionDto getGeoDistributionForVacancies(int periodDays) {
        logger.info("Getting geo distribution for vacancies, period: {} days", periodDays);
        List<CityCountDto> data = analyticsRepository.getGeoDistribution("VACANCY", periodDays);
        return new GeoDistributionDto(data);
    }

    public GeoDistributionDto getGeoDistributionForApplicants(int periodDays) {
        logger.info("Getting geo distribution for applicants, period: {} days", periodDays);
        List<CityCountDto> data = analyticsRepository.getGeoDistribution("APPLICANT", periodDays);
        return new GeoDistributionDto(data);
    }

    public ConversionStatsDto getVacancyConversion(int vacancyId, int hrUserId) {
        logger.info("Getting conversion stats for vacancy: {}, hr: {}", vacancyId, hrUserId);
        
        Optional<?> vacancyOpt = vacancyRepository.findById(vacancyId);
        if (vacancyOpt.isEmpty()) {
            throw new RuntimeException("Vacancy not found: " + vacancyId);
        }
        
        com.jobapp.server.model.Vacancy vacancy = (com.jobapp.server.model.Vacancy) vacancyOpt.get();
        Optional<com.jobapp.server.model.Company> companyOpt = companyRepository.findByVacancyId(vacancyId);
        
        if (companyOpt.isEmpty()) {
            throw new RuntimeException("Company not found for vacancy: " + vacancyId);
        }
        
        com.jobapp.server.model.Company company = companyOpt.get();
        if (company.getUserId() != hrUserId) {
            throw new RuntimeException("Access denied: vacancy belongs to another HR");
        }
        
        return analyticsRepository.getVacancyConversion(vacancyId);
    }

    public List<ChartDataPointDto> getSalaryComparisonForTags(List<Integer> tagIds) {
        logger.info("Getting salary comparison for tags: {}", tagIds);
        
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        
        return analyticsRepository.getSalaryStatsByTagIds(tagIds);
    }

    public SystemStatsDto getSystemStats() {
        logger.info("Getting system stats");
        return analyticsRepository.getSystemStats();
    }

    public List<ChartDataPointDto> getActivityTimeline(int days) {
        logger.info("Getting activity timeline for {} days", days);
        return analyticsRepository.getActivityTimeline(days);
    }

    public SalaryStatsDto getAllSalaryStats(int periodDays) {
        logger.info("Getting all salary stats, period: {} days", periodDays);
        List<ChartDataPointDto> dataPoints = analyticsRepository.getAllSalaryStats(periodDays);

        double average = dataPoints.stream()
                .mapToDouble(ChartDataPointDto::getValue)
                .average()
                .orElse(0.0);

        String period = periodDays + " days";
        return new SalaryStatsDto(dataPoints, period, average);
    }
}