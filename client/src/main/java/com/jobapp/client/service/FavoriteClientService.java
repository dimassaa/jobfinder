package com.jobapp.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.FavoriteRequest;
import com.jobapp.common.dto.VacancyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FavoriteClientService {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteClientService.class);

    private final NetworkService networkService;
    private final ObjectMapper objectMapper;

    public FavoriteClientService(NetworkService networkService) {
        this.networkService = networkService;
        this.objectMapper = new ObjectMapper();
    }

    public void add(int vacancyId) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "ADD_FAVORITE", vacancyId);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Vacancy {} added to favorites", vacancyId);
    }

    public void remove(int vacancyId) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "REMOVE_FAVORITE", vacancyId);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Vacancy {} removed from favorites", vacancyId);
    }

    public List<VacancyDto> getFavorites() throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "GET_FAVORITES", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, VacancyDto.class));
    }

    public boolean isFavorite(int vacancyId) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        FavoriteRequest request = new FavoriteRequest();
        request.setVacancyId(vacancyId);

        Message message = new Message("REQUEST", "CHECK_FAVORITE", request);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return Boolean.TRUE.equals(response.getData());
    }
}