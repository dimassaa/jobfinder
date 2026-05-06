package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetAllVacanciesAdminCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetAllVacanciesAdminCommand.class);

    private final AdminService adminService;

    public GetAllVacanciesAdminCommand(AdminService adminService) {
        this.adminService = adminService;
    }

    @SuppressWarnings("unchecked")
    public Response execute(Message msg) {
        try {
            boolean includeInactive = false;
            Map<String, Object> data = (Map<String, Object>) msg.getData();
            if (data != null && data.containsKey("includeInactive")) {
                Object val = data.get("includeInactive");
                if (val != null) {
                    includeInactive = (Boolean) val;
                }
            }

            logger.info("GET_ALL_VACANCIES_ADMIN request: includeInactive={}", includeInactive);

            var vacancies = adminService.getAllVacancies(includeInactive);
            logger.info("GET_ALL_VACANCIES_ADMIN found {} vacancies", vacancies.size());
            return new Response("OK", null, vacancies);
        } catch (Exception e) {
            logger.error("Error processing GET_ALL_VACANCIES_ADMIN command", e);
            return new Response("ERROR", "Failed to retrieve vacancies: " + e.getMessage(), null);
        }
    }
}