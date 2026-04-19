package com.jobapp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public Server() {
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            logger.info("Server started on port {}", PORT);
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
            logger.error("Failed to start server on port {}", PORT, e);
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
        threadPool.shutdown();
        logger.info("Server stopped");
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}