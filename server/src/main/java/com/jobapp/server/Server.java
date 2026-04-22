package com.jobapp.server;

import com.jobapp.server.db.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final int DEFAULT_PORT = 8081;
    private static final int THREAD_POOL_SIZE = 10;
    private final int port;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public Server() {
        this(parsePortFromEnv());
    }

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    private static int parsePortFromEnv() {
        String envPort = System.getenv("SERVER_PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                int port = Integer.parseInt(envPort.trim());
                if (port > 0 && port < 65536) {
                    logger.info("Using port from SERVER_PORT env: {}", port);
                    return port;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid SERVER_PORT value: {}, using default", envPort);
            }
        }
        logger.info("Using default port: {}", DEFAULT_PORT);
        return DEFAULT_PORT;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            running = true;
            logger.info("Server started on port {}", port);
            logger.info("Thread pool size: {}", THREAD_POOL_SIZE);

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    logger.info("New connection from {}", socket.getInetAddress());
                    ClientHandler handler = new ClientHandler(socket);
                    threadPool.execute(handler);
                } catch (IOException e) {
                    if (running) {
                        logger.error("Error accepting connection", e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to start server on port {}", port, e);
        } finally {
            shutdown();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }
    }

    private void shutdown() {
        logger.info("Shutting down server...");
        DatabaseConfig.close();
        threadPool.shutdown();
        logger.info("Server stopped");
    }

    public static void main(String[] args) {
        DatabaseConfig.getDataSource();
        Server server = new Server();
        server.start();
    }
}