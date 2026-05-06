package com.jobapp.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.AdminUserDto;
import com.jobapp.common.dto.BlockUserRequest;
import com.jobapp.common.dto.ComplaintAdminDto;
import com.jobapp.common.dto.ResolveComplaintRequest;

import com.jobapp.common.dto.VacancyDto;
import com.jobapp.common.dto.TagDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminClientService {

    private static final Logger logger = LoggerFactory.getLogger(AdminClientService.class);

    private final NetworkService networkService;
    private final ObjectMapper objectMapper;

    public AdminClientService(NetworkService networkService) {
        this.networkService = networkService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public List<AdminUserDto> getAllUsers(String role, Boolean isBlocked, int page, int size) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("roleFilter", role);
        data.put("blockFilter", isBlocked);
        data.put("page", page);
        data.put("size", size);

        Message message = new Message("REQUEST", "GET_ALL_USERS", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, AdminUserDto.class));
    }

    public void blockUser(int userId, String reason, Integer durationHours) throws Exception {
        BlockUserRequest request = new BlockUserRequest();
        request.setUserId(userId);
        request.setReason(reason);
        request.setDurationHours(durationHours);

        Message message = new Message("REQUEST", "BLOCK_USER", request);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("User {} blocked successfully", userId);
    }

    public void unblockUser(int userId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        Message message = new Message("REQUEST", "UNBLOCK_USER", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("User {} unblocked successfully", userId);
    }

    public void deleteUser(int userId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        Message message = new Message("REQUEST", "DELETE_USER", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("User {} deleted successfully", userId);
    }

    public List<VacancyDto> getAllVacancies(boolean includeInactive) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("includeInactive", includeInactive);
        Message message = new Message("REQUEST", "GET_ALL_VACANCIES_ADMIN", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, VacancyDto.class));
    }

    public void deleteVacancy(int vacancyId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("vacancyId", vacancyId);
        Message message = new Message("REQUEST", "DELETE_VACANCY_ADMIN", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Vacancy {} deleted by admin", vacancyId);
    }

    public List<ComplaintAdminDto> getComplaints() throws Exception {
        Message message = new Message("REQUEST", "GET_COMPLAINTS_ADMIN", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, ComplaintAdminDto.class));
    }

    public void resolveComplaint(int complaintId, String action) throws Exception {
        ResolveComplaintRequest request = new ResolveComplaintRequest();
        request.setComplaintId(complaintId);
        request.setAction(action);

        Message message = new Message("REQUEST", "RESOLVE_COMPLAINT", request);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Complaint {} resolved with action {}", complaintId, action);
    }

    public void addTag(String name) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("tagName", name);
        Message message = new Message("REQUEST", "ADD_TAG", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Tag {} added successfully", name);
    }

    public void deactivateTag(int tagId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("tagId", tagId);
        Message message = new Message("REQUEST", "DEACTIVATE_TAG", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Tag {} deactivated successfully", tagId);
    }

    public void createAdmin(String email, String password, String name) throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);
        data.put("name", name);

        Message message = new Message("REQUEST", "CREATE_ADMIN", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Admin {} created successfully", email);
    }

    public List<TagDto> getAllTags() throws Exception {
        Message message = new Message("REQUEST", "GET_ALL_TAGS", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, TagDto.class));
    }
}