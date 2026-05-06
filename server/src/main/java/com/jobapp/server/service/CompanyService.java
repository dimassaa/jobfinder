package com.jobapp.server.service;

import com.jobapp.server.model.Company;
import com.jobapp.server.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Map<String, String> getCompanyProfile(int userId) {
        logger.debug("Getting company profile for user: {}", userId);
        Optional<Company> companyOpt = companyRepository.findByUserId(userId);

        if (companyOpt.isEmpty()) {
            logger.info("Company not found for user: {}", userId);
            return null;
        }

        Company company = companyOpt.get();
        Map<String, String> profile = new HashMap<>();
        profile.put("companyName", company.getCompanyName() != null ? company.getCompanyName() : "");
        profile.put("website", company.getWebsite() != null ? company.getWebsite() : "");
        profile.put("description", company.getDescription() != null ? company.getDescription() : "");

        return profile;
    }

    public void saveCompanyProfile(int userId, Map<String, String> data) {
        logger.debug("Saving company profile for user: {}", userId);

        String companyName = data.get("companyName");
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }

        Optional<Company> existingOpt = companyRepository.findByUserId(userId);

        if (existingOpt.isPresent()) {
            Company company = existingOpt.get();
            company.setCompanyName(companyName);
            company.setWebsite(data.get("website"));
            company.setDescription(data.get("description"));
            companyRepository.update(company);
            logger.info("Company profile updated for user: {}", userId);
        } else {
            Company company = new Company();
            company.setUserId(userId);
            company.setCompanyName(companyName);
            company.setWebsite(data.get("website"));
            company.setDescription(data.get("description"));
            companyRepository.save(company);
            logger.info("Company profile created for user: {}", userId);
        }
    }
}