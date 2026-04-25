package com.jobapp.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CreateAdminCommand {
    private static final Logger logger = LoggerFactory.getLogger(CreateAdminCommand.class);

    private final AdminService adminService;
    private final ObjectMapper objectMapper;

    public CreateAdminCommand(AdminService adminService, ObjectMapper objectMapper) {
        this.adminService = adminService;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public Response execute(Message msg) {
        try {
            Map<String, Object> data = (Map<String, Object>) msg.getData();
            String email = (String) data.get("email");
            String password = (String) data.get("password");
            String name = (String) data.get("name");

            if (email == null || email.trim().isEmpty()) {
                return new Response("ERROR", "Email is required", null);
            }
            if (password == null || password.length() < 6) {
                return new Response("ERROR", "Password must be at least 6 characters", null);
            }
            if (name == null || name.trim().isEmpty()) {
                return new Response("ERROR", "Name is required", null);
            }

            adminService.createAdmin(email.trim(), password, name.trim());
            return new Response("OK", "Admin created successfully", null);
        } catch (IllegalArgumentException e) {
            logger.warn("CREATE_ADMIN failed: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error processing CREATE_ADMIN command", e);
            return new Response("ERROR", "Failed to create admin", null);
        }
    }
}