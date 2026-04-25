package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UnblockUserCommand {
    private static final Logger logger = LoggerFactory.getLogger(UnblockUserCommand.class);

    private final AdminService adminService;

    public UnblockUserCommand(AdminService adminService) {
        this.adminService = adminService;
    }

    @SuppressWarnings("unchecked")
    public Response execute(Message msg) {
        try {
            Map<String, Object> data = (Map<String, Object>) msg.getData();
            Integer userId = (Integer) data.get("userId");

            if (userId == null) {
                return new Response("ERROR", "User ID is required", null);
            }

            adminService.unblockUser(userId);
            return new Response("OK", "User has been unblocked", null);
        } catch (Exception e) {
            logger.error("Error processing UNBLOCK_USER command", e);
            return new Response("ERROR", "Failed to unblock user", null);
        }
    }
}