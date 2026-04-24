package com.jobapp.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.NotificationDto;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

public class NetworkService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NetworkService.class);

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final ObjectMapper objectMapper;
    private volatile boolean connected;
    private String host;
    private int port;
    private Consumer<NotificationDto> notificationCallback;

    public NetworkService() {
        this.objectMapper = new ObjectMapper();
        this.host = AppContext.getServerHost();
        this.port = AppContext.getServerPort();
        logger.info("NetworkService initialized with {}:{}", host, port);
    }

    public void connect() throws IOException {
        connect(host, port);
    }

    public void connect(String host, int port) throws IOException {
        disconnect();

        this.host = host;
        this.port = port;

        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        connected = true;

        logger.info("Connected to {}:{}", host, port);
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void sendMessage(Message msg) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to server");
        }

        String json = objectMapper.writeValueAsString(msg);
        writer.println(json);
        writer.flush();

        logger.info("Sent message: {}", msg.getCommand());
    }

    public Response sendAndReceive(Message msg) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to server");
        }

        String json = objectMapper.writeValueAsString(msg);
        writer.println(json);
        writer.flush();

        logger.info("Sent message and waiting for response: {}", msg.getCommand());

        String responseJson = reader.readLine();
        if (responseJson == null) {
            throw new IOException("Server closed connection");
        }

        return objectMapper.readValue(responseJson, Response.class);
    }

    public void startListening(Consumer<Response> onResponse) {
        Task<Void> listeningTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                while (isConnected() && !Thread.currentThread().isInterrupted()) {
                    try {
                        String line = reader.readLine();
                        if (line == null) {
                            logger.info("Server closed connection");
                            connected = false;
                            break;
                        }

                        JsonNode node = objectMapper.readTree(line);
                        String type = node.has("type") ? node.get("type").asText() : null;

                        if ("NOTIFICATION".equals(type) && notificationCallback != null) {
                            NotificationDto notification = objectMapper.readValue(line, NotificationDto.class);
                            logger.info("Received push notification: {}", notification.getTitle());

                            javafx.application.Platform.runLater(() -> notificationCallback.accept(notification));
                        } else {
                            Response response = objectMapper.readValue(line, Response.class);
                            logger.info("Received response: {}", response.getStatus());

                            javafx.application.Platform.runLater(() -> onResponse.accept(response));
                        }

                    } catch (SocketException e) {
                        logger.warn("Socket exception: {}", e.getMessage());
                        connected = false;
                        break;
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to parse response: {}", e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void failed() {
                logger.error("Listening task failed: {}", getException().getMessage());
                connected = false;
            }
        };

        Thread backgroundThread = new Thread(listeningTask);
        backgroundThread.setDaemon(true);
        backgroundThread.start();

        logger.info("Started listening for responses");
    }

    public void disconnect() {
        connected = false;

        if (writer != null) {
            writer.close();
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                logger.warn("Error closing reader: {}", e.getMessage());
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn("Error closing socket: {}", e.getMessage());
            }
        }

        logger.info("Disconnected from server");
    }

    public void reconnect(String host, int port) {
        logger.info("Attempting to reconnect to {}:{}...", host, port);
        disconnect();

        try {
            Thread.sleep(1000);
            connect(host, port);
            logger.info("Reconnected successfully to {}:{}", host, port);
        } catch (IOException e) {
            logger.error("Reconnection failed: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Reconnection interrupted");
        }
    }

    public void reconnect() {
        reconnect(host, port);
    }

    public void setNotificationCallback(Consumer<NotificationDto> callback) {
        this.notificationCallback = callback;
    }

    public boolean hasNotificationCallback() {
        return notificationCallback != null;
    }
}