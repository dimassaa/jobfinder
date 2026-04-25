package com.jobapp.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.BlockUserRequest;
import com.jobapp.server.service.AdminService;
import com.jobapp.server.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockUserCommand {
    private static final Logger logger = LoggerFactory.getLogger(BlockUserCommand.class);

    private final AdminService adminService;
    private final ObjectMapper objectMapper;
    private final int adminId;

    public BlockUserCommand(AdminService adminService, ObjectMapper objectMapper, int adminId) {
        this.adminService = adminService;
        this.objectMapper = objectMapper;
        this.adminId = adminId;
    }

    public Response execute(Message msg) {
        try {
            String dataJson = objectMapper.writeValueAsString(msg.getData());
            BlockUserRequest request = objectMapper.readValue(dataJson, BlockUserRequest.class);

            String idError = ValidationUtil.validateId(request.getUserId(), "User ID");
            if (idError != null) {
                return new Response("ERROR", idError, null);
            }

            if (request.getUserId() == adminId) {
                return new Response("ERROR", "Cannot block yourself", null);
            }

            adminService.blockUser(request.getUserId(), adminId, request.getReason(), request.getDurationHours());
            return new Response("OK", "User has been blocked", null);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error processing BLOCK_USER command", e);
            return new Response("ERROR", "Failed to block user", null);
        }
    }
}