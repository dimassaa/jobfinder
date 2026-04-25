package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DeactivateTagCommand {
    private static final Logger logger = LoggerFactory.getLogger(DeactivateTagCommand.class);

    private final AdminService adminService;

    public DeactivateTagCommand(AdminService adminService) {
        this.adminService = adminService;
    }

    @SuppressWarnings("unchecked")
    public Response execute(Message msg) {
        try {
            Map<String, Object> data = (Map<String, Object>) msg.getData();
            Integer tagId = (Integer) data.get("tagId");

            if (tagId == null) {
                return new Response("ERROR", "Tag ID is required", null);
            }

            adminService.deactivateTag(tagId);
            return new Response("OK", "Tag has been deactivated", null);
        } catch (Exception e) {
            logger.error("Error processing DEACTIVATE_TAG command", e);
            return new Response("ERROR", "Failed to deactivate tag", null);
        }
    }
}