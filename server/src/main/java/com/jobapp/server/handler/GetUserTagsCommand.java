package com.jobapp.server.handler;

import com.jobapp.common.Response;
import com.jobapp.common.dto.TagDto;
import com.jobapp.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetUserTagsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetUserTagsCommand.class);

    private final UserService userService;
    private final int userId;

    public GetUserTagsCommand(UserService userService, int userId) {
        this.userService = userService;
        this.userId = userId;
    }

    public Response execute() {
        logger.info("Executing GET_USER_TAGS for user: {}", userId);
        try {
            List<TagDto> tags = userService.getUserTags(userId);
            return new Response("OK", "Tags retrieved", tags);
        } catch (Exception e) {
            logger.error("Error retrieving tags for user: {}", userId, e);
            return new Response("ERROR", "Failed to retrieve tags", null);
        }
    }
}