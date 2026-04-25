package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DeleteVacancyAdminCommand {
    private static final Logger logger = LoggerFactory.getLogger(DeleteVacancyAdminCommand.class);

    private final AdminService adminService;

    public DeleteVacancyAdminCommand(AdminService adminService) {
        this.adminService = adminService;
    }

    @SuppressWarnings("unchecked")
    public Response execute(Message msg) {
        try {
            Map<String, Object> data = (Map<String, Object>) msg.getData();
            Integer vacancyId = (Integer) data.get("vacancyId");

            if (vacancyId == null) {
                return new Response("ERROR", "Vacancy ID is required", null);
            }

            adminService.deleteVacancy(vacancyId);
            return new Response("OK", "Vacancy has been deleted", null);
        } catch (Exception e) {
            logger.error("Error processing DELETE_VACANCY_ADMIN command", e);
            return new Response("ERROR", "Failed to delete vacancy", null);
        }
    }
}