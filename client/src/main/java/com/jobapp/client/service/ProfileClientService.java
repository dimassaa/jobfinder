package com.jobapp.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.model.UserProfile;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ProfileDto;
import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.UserTagsUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfileClientService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileClientService.class);

    private final NetworkService networkService;
    private final ObjectMapper objectMapper;

    public ProfileClientService(NetworkService networkService) {
        this.networkService = networkService;
        this.objectMapper = new ObjectMapper();
    }

    public UserProfile getProfile() throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "GET_PROFILE", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        ProfileDto profileDto = objectMapper.convertValue(response.getData(), ProfileDto.class);
        return mapToUserProfile(profileDto);
    }

    public void updateProfile(UserProfile profile) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        ProfileDto dto = mapToProfileDto(profile);
        Message message = new Message("REQUEST", "UPDATE_PROFILE", dto);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Profile updated successfully");
    }

    public List<TagDto> getUserTags() throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        Message message = new Message("REQUEST", "GET_USER_TAGS", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, TagDto.class));
    }

    public void updateUserTags(List<Integer> tagIds) throws Exception {
        Integer userId = AppContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User not logged in");
        }

        UserTagsUpdateRequest request = new UserTagsUpdateRequest(tagIds);
        Message message = new Message("REQUEST", "UPDATE_USER_TAGS", request);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("User tags updated successfully");
    }

    public List<TagDto> getAllActiveTags() throws Exception {
        Message message = new Message("REQUEST", "GET_ALL_TAGS", null);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        return objectMapper.convertValue(response.getData(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, TagDto.class));
    }

    private UserProfile mapToUserProfile(ProfileDto dto) {
        UserProfile profile = new UserProfile();
        profile.setName(dto.getName());
        profile.setSurname(dto.getSurname());
        profile.setCity(dto.getCity());
        profile.setPhone(dto.getPhone());
        profile.setAdditionalInfo(dto.getAdditionalInfo());
        profile.setCompanyId(dto.getCompanyId());
        profile.setCompanyName(dto.getCompanyName());
        profile.setCompanyWebsite(dto.getCompanyWebsite());
        profile.setCompanyDescription(dto.getCompanyDescription());
        profile.setTags(dto.getTags());
        return profile;
    }

    private ProfileDto mapToProfileDto(UserProfile profile) {
        ProfileDto dto = new ProfileDto(
            profile.getName(),
            profile.getSurname(),
            profile.getCity(),
            profile.getPhone(),
            profile.getAdditionalInfo()
        );
        dto.setCompanyId(profile.getCompanyId());
        dto.setCompanyName(profile.getCompanyName());
        dto.setCompanyWebsite(profile.getCompanyWebsite());
        dto.setCompanyDescription(profile.getCompanyDescription());
        return dto;
    }
}