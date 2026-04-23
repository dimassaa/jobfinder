package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ProfileDto;
import com.jobapp.server.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateProfileCommand {
    private static final Logger logger = LoggerFactory.getLogger(UpdateProfileCommand.class);

    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final int userId;

    public UpdateProfileCommand(UserService userService, ObjectMapper objectMapper, int userId) {
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing UPDATE_PROFILE for user: {}", userId);
        try {
            String dataJson = objectMapper.writeValueAsString(msg.getData());
            ProfileDto profileDto = objectMapper.readValue(dataJson, ProfileDto.class);

            userService.updateProfile(userId, profileDto);
            return new Response("OK", "Profile updated successfully", profileDto);
        } catch (Exception e) {
            logger.error("Error updating profile for user: {}", userId, e);
            return new Response("ERROR", "Failed to update profile", null);
        }
    }
}