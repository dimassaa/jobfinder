package com.jobapp.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.VacancyCreateRequest;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.common.dto.VacancyFilterDto;
import com.jobapp.common.dto.VacancyUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VacancyClientService {

    private static final Logger logger = LoggerFactory.getLogger(VacancyClientService.class);

    private final NetworkService networkService;
    private final ObjectMapper objectMapper;

    public VacancyClientService(NetworkService networkService) {
        this.networkService = networkService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public List<VacancyDto> search(VacancyFilterDto filter) throws Exception {
        Message message = new Message("REQUEST", "SEARCH_VACANCIES", filter);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, VacancyDto.class));
    }

    public VacancyDto getDetails(int vacancyId) throws Exception {
        Message message = new Message("REQUEST", "GET_VACANCY_DETAILS", vacancyId);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(), VacancyDto.class);
    }

    public VacancyDto create(VacancyCreateRequest request) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "CREATE_VACANCY", request);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Vacancy created successfully");
        return objectMapper.convertValue(response.getData(), VacancyDto.class);
    }

    public void update(int vacancyId, VacancyCreateRequest request) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        VacancyUpdateRequest updateRequest = new VacancyUpdateRequest();
        updateRequest.setVacancyId(vacancyId);
        updateRequest.setVacancy(request);

        Message message = new Message("REQUEST", "UPDATE_VACANCY", updateRequest);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Vacancy {} updated successfully", vacancyId);
    }

    public void close(int vacancyId) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "CLOSE_VACANCY", vacancyId);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Vacancy {} closed successfully", vacancyId);
    }

    public List<VacancyDto> getMyVacancies() throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "GET_MY_VACANCIES", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, VacancyDto.class));
    }
}