package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DeleteUserCommand {
    private static final Logger logger = LoggerFactory.getLogger(DeleteUserCommand.class);

    private final AdminService adminService;

    public DeleteUserCommand(AdminService adminService) {
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

            adminService.deleteUser(userId);
            return new Response("OK", "User has been deleted", null);
        } catch (Exception e) {
            logger.error("Error processing DELETE_USER command", e);
            return new Response("ERROR", "Failed to delete user", null);
        }
    }
}