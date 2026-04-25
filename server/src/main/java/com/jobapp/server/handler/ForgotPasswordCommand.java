package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForgotPasswordCommand {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordCommand.class);

    private final PasswordResetService passwordResetService;

    public ForgotPasswordCommand(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    public Response execute(Message msg) {
        try {
            String email = (String) msg.getData();
            if (email == null || email.trim().isEmpty()) {
                return new Response("ERROR", "Email is required", null);
            }
            passwordResetService.requestPasswordReset(email.trim());
            return new Response("OK", "Password reset instructions sent to email", null);
        } catch (Exception e) {
            logger.error("Error processing FORGOT_PASSWORD command", e);
            return new Response("ERROR", "Failed to process password reset request", null);
        }
    }
}