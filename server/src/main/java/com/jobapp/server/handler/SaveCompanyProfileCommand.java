package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SaveCompanyProfileCommand {
    private static final Logger logger = LoggerFactory.getLogger(SaveCompanyProfileCommand.class);

    private final CompanyService companyService;
    private final int userId;

    public SaveCompanyProfileCommand(CompanyService companyService, int userId) {
        this.companyService = companyService;
        this.userId = userId;
    }

    public Response execute(Message msg) {
        logger.info("Executing SAVE_COMPANY_PROFILE for user: {}", userId);
        try {
            Object data = msg.getData();
            Map<String, String> profileData;

            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> dataMap = (Map<String, String>) data;
                profileData = dataMap;
            } else {
                return new Response("ERROR", "Invalid data format", null);
            }

            companyService.saveCompanyProfile(userId, profileData);
            return new Response("OK", "Company profile saved successfully", null);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid company profile data: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error saving company profile for user: {}", userId, e);
            return new Response("ERROR", "Failed to save company profile: " + e.getMessage(), null);
        }
    }
}