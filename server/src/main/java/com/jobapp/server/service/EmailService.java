package com.jobapp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void send(String to, String subject, String body) {
        logger.info("=== EMAIL ===");
        logger.info("To: {}", to);
        logger.info("Subject: {}", subject);
        logger.info("Body: {}", body);
        logger.info("=============");
    }
}