package com.jobapp.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.ResolveComplaintRequest;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolveComplaintCommand {
    private static final Logger logger = LoggerFactory.getLogger(ResolveComplaintCommand.class);

    private final AdminService adminService;
    private final ObjectMapper objectMapper;
    private final int adminId;

    public ResolveComplaintCommand(AdminService adminService, ObjectMapper objectMapper, int adminId) {
        this.adminService = adminService;
        this.objectMapper = objectMapper;
        this.adminId = adminId;
    }

    public Response execute(Message msg) {
        try {
            String dataJson = objectMapper.writeValueAsString(msg.getData());
            ResolveComplaintRequest request = objectMapper.readValue(dataJson, ResolveComplaintRequest.class);

            if (request.getComplaintId() == null) {
                return new Response("ERROR", "Complaint ID is required", null);
            }
            if (request.getAction() == null || request.getAction().trim().isEmpty()) {
                return new Response("ERROR", "Action is required", null);
            }

            adminService.resolveComplaint(request.getComplaintId(), request.getAction(), adminId);
            return new Response("OK", "Complaint has been resolved", null);
        } catch (Exception e) {
            logger.error("Error processing RESOLVE_COMPLAINT command", e);
            return new Response("ERROR", "Failed to resolve complaint", null);
        }
    }
}