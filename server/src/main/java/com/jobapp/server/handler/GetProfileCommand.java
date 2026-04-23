package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ProfileDto;
import com.jobapp.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetProfileCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetProfileCommand.class);

    private final UserService userService;
    private final int userId;

    public GetProfileCommand(UserService userService, int userId) {
        this.userService = userService;
        this.userId = userId;
    }

    public Response execute() {
        logger.info("Executing GET_PROFILE for user: {}", userId);
        try {
            ProfileDto profile = userService.getProfile(userId);
            if (profile == null) {
                return new Response("ERROR", "User not found", null);
            }
            return new Response("OK", "Profile retrieved", profile);
        } catch (Exception e) {
            logger.error("Error retrieving profile for user: {}", userId, e);
            return new Response("ERROR", "Failed to retrieve profile", null);
        }
    }
}