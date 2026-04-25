package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AddTagCommand {
    private static final Logger logger = LoggerFactory.getLogger(AddTagCommand.class);

    private final AdminService adminService;

    public AddTagCommand(AdminService adminService) {
        this.adminService = adminService;
    }

    @SuppressWarnings("unchecked")
    public Response execute(Message msg) {
        try {
            Map<String, Object> data = (Map<String, Object>) msg.getData();
            String tagName = (String) data.get("tagName");

            if (tagName == null || tagName.trim().isEmpty()) {
                return new Response("ERROR", "Tag name is required", null);
            }

            adminService.addTag(tagName.trim());
            return new Response("OK", "Tag has been added", null);
        } catch (IllegalArgumentException e) {
            logger.warn("ADD_TAG failed: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error processing ADD_TAG command", e);
            return new Response("ERROR", "Failed to add tag", null);
        }
    }
}