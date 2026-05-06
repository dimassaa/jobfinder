package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetCompanyProfileCommand {
    private static final Logger logger = LoggerFactory.getLogger(GetCompanyProfileCommand.class);

    private final CompanyService companyService;
    private final int userId;

    public GetCompanyProfileCommand(CompanyService companyService, int userId) {
        this.companyService = companyService;
        this.userId = userId;
    }

    public Response execute() {
        logger.info("Executing GET_COMPANY_PROFILE for user: {}", userId);
        try {
            Map<String, String> profile = companyService.getCompanyProfile(userId);
            if (profile == null) {
                return new Response("OK", "Company profile not found. Please create one.", null);
            }
            return new Response("OK", "Company profile retrieved", profile);
        } catch (Exception e) {
            logger.error("Error getting company profile for user: {}", userId, e);
            return new Response("ERROR", "Failed to retrieve company profile: " + e.getMessage(), null);
        }
    }
}