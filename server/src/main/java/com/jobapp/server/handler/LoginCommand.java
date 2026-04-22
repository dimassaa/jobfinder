package com.jobapp.server.handler;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.AuthResponse;
import com.jobapp.common.dto.LoginRequest;
import com.jobapp.server.service.AuthService;
import com.jobapp.server.service.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

public class LoginCommand {
    private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class);

    private final AuthService authService;
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public LoginCommand(AuthService authService, SessionManager sessionManager, ObjectMapper objectMapper) {
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    public Response execute(Message msg, PrintWriter out) {
        try {
            String dataJson = objectMapper.writeValueAsString(msg.getData());
            LoginRequest request = objectMapper.readValue(dataJson, LoginRequest.class);
            AuthResponse authResponse = authService.login(request);

            if (authResponse.isSuccess()) {
                sessionManager.addSession(authResponse.getUserId(), out);
                logger.info("User {} logged in, session created", authResponse.getUserId());
            }

            return new Response(
                    authResponse.isSuccess() ? "OK" : "ERROR",
                    authResponse.getMessage(),
                    authResponse
            );
        } catch (Exception e) {
            logger.error("Error processing LOGIN command", e);
            return new Response("ERROR", "Invalid login request format", null);
        }
    }
}