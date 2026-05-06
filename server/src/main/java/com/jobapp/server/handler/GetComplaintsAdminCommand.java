package com.jobapp.server.handler;

import com.jobapp.common.Response;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetComplaintsAdminCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetComplaintsAdminCommand.class);

    private final AdminService adminService;

    public GetComplaintsAdminCommand(AdminService adminService) {
        this.adminService = adminService;
    }

    public Response execute() {
        try {
            logger.info("GET_COMPLAINTS_ADMIN request");
            var complaints = adminService.getComplaints();
            logger.info("GET_COMPLAINTS_ADMIN found {} complaints", complaints.size());
            return new Response("OK", null, complaints);
        } catch (Exception e) {
            logger.error("Error processing GET_COMPLAINTS_ADMIN command", e);
            return new Response("ERROR", "Failed to retrieve complaints: " + e.getMessage(), null);
        }
    }
}