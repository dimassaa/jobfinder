package com.jobapp.client.service;

import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PasswordResetClientService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetClientService.class);

    private final NetworkService networkService;

    public PasswordResetClientService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void requestPasswordReset(String email) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }

        Message message = new Message("REQUEST", "FORGOT_PASSWORD", email.trim());
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Password reset requested for email: {}", email);
    }

    public void resetPassword(String token, String newPassword) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Токен не может быть пустым");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов");
        }

        Map<String, String> data = new HashMap<>();
        data.put("token", token.trim());
        data.put("newPassword", newPassword);

        Message message = new Message("REQUEST", "RESET_PASSWORD", data);
        Response response = networkService.sendAndReceive(message);

        if (!"OK".equals(response.getStatus())) {
            throw new RuntimeException(response.getMessage());
        }

        logger.info("Password reset completed successfully");
    }
}