package com.jobapp.server;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.handler.LoginCommand;
import com.jobapp.server.handler.RegisterCommand;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.AuthService;
import com.jobapp.server.service.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socket;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AuthService authService;
    private final SessionManager sessionManager;
    private final LoginCommand loginCommand;
    private final RegisterCommand registerCommand;
    private Integer currentUserId;
    private boolean shouldClose = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.objectMapper = new ObjectMapper();
        this.userRepository = new UserRepository();
        this.companyRepository = new CompanyRepository();
        this.sessionManager = SessionManager.getInstance();
        this.authService = new AuthService(userRepository, companyRepository);
        this.loginCommand = new LoginCommand(authService, sessionManager, objectMapper);
        this.registerCommand = new RegisterCommand(authService, objectMapper);
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while (!shouldClose && (line = in.readLine()) != null) {
                logger.info("Received from {}: {}", socket.getInetAddress(), line);

                if ("exit".equalsIgnoreCase(line)) {
                    logger.info("Client {} requested disconnect", socket.getInetAddress());
                    break;
                }

                try {
                    Message msg = objectMapper.readValue(line, Message.class);
                    String responseJson = processMessage(msg, out);
                    out.println(responseJson);
                    logger.info("Sent to {}: {}", socket.getInetAddress(), responseJson);
                } catch (Exception e) {
                    logger.error("Error processing message from {}", socket.getInetAddress(), e);
                    Response errorResponse = new Response("ERROR", "Invalid message format", null);
                    out.println(objectMapper.writeValueAsString(errorResponse));
                }
            }
        } catch (IOException e) {
            logger.error("Connection error with {}", socket.getInetAddress(), e);
        } finally {
            if (currentUserId != null) {
                sessionManager.removeSession(currentUserId);
            }
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error closing socket", e);
            }
        }
    }

    private String processMessage(Message msg, PrintWriter out) throws IOException {
        String command = msg.getCommand();
        logger.info("Received command: {} from {}", command, socket.getInetAddress());

        Response response;

        switch (command != null ? command.toUpperCase() : "") {
            case "PING":
                response = new Response("OK", null, "PONG");
                break;
            case "LOGIN":
                response = loginCommand.execute(msg, out);
                if (response.getData() != null) {
                    var authResp = (com.jobapp.common.dto.AuthResponse) response.getData();
                    if (authResp.isSuccess()) {
                        currentUserId = authResp.getUserId();
                    }
                }
                break;
            case "REGISTER":
                response = registerCommand.execute(msg);
                break;
            case "TEST_DB":
                var testUser = userRepository.findByEmail("test@example.com");
                if (testUser.isPresent()) {
                    response = new Response("OK", "User found: " + testUser.get().getEmail(), testUser.get());
                } else {
                    response = new Response("OK", "No users found in database", null);
                }
                break;
            case "EXIT":
                response = new Response("OK", "Goodbye", null);
                shouldClose = true;
                break;
            case "LOGOUT":
                if (currentUserId != null) {
                    sessionManager.removeSession(currentUserId);
                    logger.info("User {} logged out", currentUserId);
                    currentUserId = null;
                }
                response = new Response("OK", "Logged out successfully", null);
                shouldClose = true;
                break;
            default:
                response = new Response("OK", "Echo: " + command, msg.getData());
        }

        return objectMapper.writeValueAsString(response);
    }
}