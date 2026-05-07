package com.jobapp.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AnalyticsClientService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsClientService.class);

    private final NetworkService networkService;
    private final ObjectMapper objectMapper;

    public AnalyticsClientService(NetworkService networkService) {
        this.networkService = networkService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public SalaryStatsDto getSalaryStats(int periodDays) throws Exception {
        logger.info("Fetching salary statistics, period: {} days", periodDays);
        Map<String, Object> data = new HashMap<>();
        data.put("periodDays", periodDays);

        Message message = new Message("REQUEST", "GET_SALARY_STATS", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(), SalaryStatsDto.class);
    }

    public List<TagUsageDto> getTopTags(int periodDays, int limit) throws Exception {
        logger.info("Fetching top tags, period: {} days, limit: {}", periodDays, limit);
        Map<String, Object> data = new HashMap<>();
        data.put("periodDays", periodDays);
        data.put("limit", limit);

        Message message = new Message("REQUEST", "GET_TOP_TAGS", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, TagUsageDto.class));
    }

    public GeoDistributionDto getGeoStats(String type, int periodDays) throws Exception {
        logger.info("Fetching geo statistics, type: {}, period: {} days", type, periodDays);
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("periodDays", periodDays);

        Message message = new Message("REQUEST", "GET_GEO_STATS", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(), GeoDistributionDto.class);
    }

    public ConversionStatsDto getConversion(int vacancyId) throws Exception {
        logger.info("Fetching conversion stats for vacancy: {}", vacancyId);
        Map<String, Object> data = new HashMap<>();
        data.put("vacancyId", vacancyId);

        Message message = new Message("REQUEST", "GET_CONVERSION", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(), ConversionStatsDto.class);
    }

    public List<ChartDataPointDto> getSalaryComparison(List<Integer> tagIds) throws Exception {
        logger.info("Fetching salary comparison for tags: {}", tagIds);
        Map<String, Object> data = new HashMap<>();
        data.put("tagIds", tagIds);

        Message message = new Message("REQUEST", "GET_SALARY_COMPARISON", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, ChartDataPointDto.class));
    }

    public SystemStatsDto getSystemStats() throws Exception {
        logger.info("Fetching system statistics");
        Message message = new Message("REQUEST", "GET_SYSTEM_STATS", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(), SystemStatsDto.class);
    }

    public List<ChartDataPointDto> getActivityTimeline(int days) throws Exception {
        logger.info("Fetching activity timeline for {} days", days);
        Map<String, Object> data = new HashMap<>();
        data.put("days", days);

        Message message = new Message("REQUEST", "GET_ACTIVITY_TIMELINE", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, ChartDataPointDto.class));
    }

    public List<TagDto> getAllTags() throws Exception {
        logger.info("Fetching all tags");
        Message message = new Message("REQUEST", "GET_ALL_TAGS", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, TagDto.class));
    }

    public SalaryStatsDto getAllSalariesStats(int periodDays) throws Exception {
        logger.info("Fetching all salaries statistics, period: {} days", periodDays);
        Map<String, Object> data = new HashMap<>();
        data.put("periodDays", periodDays);

        Message message = new Message("REQUEST", "GET_ALL_SALARY_STATS", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(), SalaryStatsDto.class);
    }
}