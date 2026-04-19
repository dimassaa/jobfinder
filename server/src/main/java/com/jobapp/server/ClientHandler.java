package com.jobapp.server;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
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

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                logger.info("Received from {}: {}", socket.getInetAddress(), line);

                if ("exit".equalsIgnoreCase(line)) {
                    logger.info("Client {} requested disconnect", socket.getInetAddress());
                    break;
                }

                try {
                    Message msg = objectMapper.readValue(line, Message.class);
                    String responseJson = processMessage(msg);
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
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error closing socket", e);
            }
        }
    }

    private String processMessage(Message msg) throws IOException {
        String command = msg.getCommand();
        Response response;

        if ("PING".equalsIgnoreCase(command)) {
            response = new Response("OK", "PONG", null);
        } else if ("LOGIN".equalsIgnoreCase(command)) {
            response = new Response("OK", "Login stub", null);
        } else if ("EXIT".equalsIgnoreCase(command)) {
            response = new Response("OK", "Goodbye", null);
        } else {
            response = new Response("OK", "Echo: " + command, msg.getData());
        }

        return objectMapper.writeValueAsString(response);
    }
}