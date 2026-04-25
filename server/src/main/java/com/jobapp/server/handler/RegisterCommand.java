package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.AuthResponse;
import com.jobapp.common.dto.RegisterRequest;
import com.jobapp.server.service.AuthService;
import com.jobapp.server.util.ValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

public class RegisterCommand {
    private static final Logger logger = LoggerFactory.getLogger(RegisterCommand.class);

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public RegisterCommand(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    public Response execute(Message msg) {
        try {
            String dataJson = objectMapper.writeValueAsString(msg.getData());
            RegisterRequest request = objectMapper.readValue(dataJson, RegisterRequest.class);

            String emailError = ValidationUtil.validateEmail(request.getEmail());
            if (emailError != null) {
                return new Response("ERROR", emailError, null);
            }

            String passwordError = ValidationUtil.validatePassword(request.getPassword());
            if (passwordError != null) {
                return new Response("ERROR", passwordError, null);
            }

            String roleError = ValidationUtil.validateRole(request.getRole());
            if (roleError != null) {
                return new Response("ERROR", roleError, null);
            }

            String nameError = ValidationUtil.validateName(request.getName(), "Name");
            if (nameError != null) {
                return new Response("ERROR", nameError, null);
            }

            String surnameError = ValidationUtil.validateName(request.getSurname(), "Surname");
            if (surnameError != null) {
                return new Response("ERROR", surnameError, null);
            }

            AuthResponse authResponse = authService.register(request);

            return new Response(
                    authResponse.isSuccess() ? "OK" : "ERROR",
                    authResponse.getMessage(),
                    authResponse
            );
        } catch (Exception e) {
            logger.error("Error processing REGISTER command", e);
            return new Response("ERROR", "Invalid register request format", null);
        }
    }
}