package com.jobapp.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ApplicationDto;
import com.jobapp.common.dto.ApplicationStatusUpdateRequest;
import com.jobapp.common.dto.FavoriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ApplicationClientService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationClientService.class);

    private final NetworkService networkService;
    private final ObjectMapper objectMapper;

    public ApplicationClientService(NetworkService networkService) {
        this.networkService = networkService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public ApplicationDto apply(int vacancyId) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        FavoriteRequest request = new FavoriteRequest();
        request.setVacancyId(vacancyId);

        Message message = new Message("REQUEST", "APPLY", request);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Application submitted successfully for vacancy {}", vacancyId);
        return objectMapper.convertValue(response.getData(), ApplicationDto.class);
    }

    public List<ApplicationDto> getAllApplicationsForHr() throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "GET_ALL_APPLICATIONS_FOR_HR", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, ApplicationDto.class));
    }

    public List<ApplicationDto> getMyApplications() throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "GET_MY_APPLICATIONS", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, ApplicationDto.class));
    }

    public List<ApplicationDto> getApplicationsForVacancy(int vacancyId) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "GET_APPLICATIONS_FOR_VACANCY", vacancyId);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, ApplicationDto.class));
    }

    public void updateStatus(int applicationId, String newStatus) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setApplicationId(applicationId);
        request.setNewStatus(newStatus);

        Message message = new Message("REQUEST", "UPDATE_APPLICATION_STATUS", request);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Application {} status updated to {}", applicationId, newStatus);
    }
}