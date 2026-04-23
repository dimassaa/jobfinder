package com.jobapp.server.handler;

import com.jobapp.common.Response;
import com.jobapp.common.dto.TagDto;
import com.jobapp.server.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetAllTagsCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetAllTagsCommand.class);

    private final TagService tagService;

    public GetAllTagsCommand(TagService tagService) {
        this.tagService = tagService;
    }

    public Response execute() {
        logger.info("Executing GET_ALL_TAGS");
        try {
            List<TagDto> tags = tagService.getAllActiveTags();
            return new Response("OK", "Tags retrieved", tags);
        } catch (Exception e) {
            logger.error("Error retrieving all tags", e);
            return new Response("ERROR", "Failed to retrieve tags", null);
        }
    }
}