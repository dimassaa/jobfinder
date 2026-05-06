package com.jobapp.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ResetPasswordCommand {
    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordCommand.class);

    private final PasswordResetService passwordResetService;
    private final ObjectMapper objectMapper;

    public ResetPasswordCommand(PasswordResetService passwordResetService, ObjectMapper objectMapper) {
        this.passwordResetService = passwordResetService;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public Response execute(Message msg) {
        try {
            Map<String, Object> data = (Map<String, Object>) msg.getData();
            if (data == null) {
                return new Response("ERROR", "Invalid request data", null);
            }

            String token = (String) data.get("token");
            String newPassword = (String) data.get("newPassword");

            if (newPassword == null || newPassword.length() < 6) {
                return new Response("ERROR", "Password must be at least 6 characters", null);
            }

            if (token != null && token.contains("@")) {
                passwordResetService.resetPasswordByEmail(token.trim(), newPassword);
            } else if (token != null && !token.trim().isEmpty()) {
                passwordResetService.resetPassword(token.trim(), newPassword);
            } else {
                return new Response("ERROR", "Email or token is required", null);
            }

            return new Response("OK", "Password has been reset successfully", null);
        } catch (IllegalArgumentException e) {
            logger.warn("Password reset failed: {}", e.getMessage());
            return new Response("ERROR", e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error processing RESET_PASSWORD command", e);
            return new Response("ERROR", "Failed to reset password", null);
        }
    }
}