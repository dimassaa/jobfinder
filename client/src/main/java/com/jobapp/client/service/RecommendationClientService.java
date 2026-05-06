package com.jobapp.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.RecommendedVacancyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RecommendationClientService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationClientService.class);

    private final NetworkService networkService;
    private final ObjectMapper objectMapper;

    public RecommendationClientService(NetworkService networkService) {
        this.networkService = networkService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public List<RecommendedVacancyDto> getRecommendations() throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "GET_RECOMMENDATIONS", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Recommendations retrieved successfully");
        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, RecommendedVacancyDto.class));
    }
}