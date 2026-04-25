package com.jobapp.server.handler;

import com.jobapp.common.Response;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetAllUsersCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetAllUsersCommand.class);

    private final AdminService adminService;

    public GetAllUsersCommand(AdminService adminService) {
        this.adminService = adminService;
    }

    @SuppressWarnings("unchecked")
    public Response execute(Map<String, Object> params) {
        try {
            String roleFilter = (String) params.get("roleFilter");
            Boolean blockFilter = params.containsKey("blockFilter") ? (Boolean) params.get("blockFilter") : null;
            int page = params.containsKey("page") ? ((Number) params.get("page")).intValue() : 0;
            int size = params.containsKey("size") ? ((Number) params.get("size")).intValue() : 20;

            var users = adminService.getAllUsers(roleFilter, blockFilter, page, size);
            return new Response("OK", null, users);
        } catch (Exception e) {
            logger.error("Error processing GET_ALL_USERS command", e);
            return new Response("ERROR", "Failed to retrieve users", null);
        }
    }
}