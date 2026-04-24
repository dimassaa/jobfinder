package com.jobapp.server;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.NotificationRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import com.jobapp.server.service.NotificationService;
import com.jobapp.server.service.ProfileReminderTask;
import com.jobapp.server.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        // 1. Check Java System Property (for tests)
        String portStr = System.getProperty("SERVER_PORT");
        if (portStr != null && !portStr.isBlank()) {
            try {
                int port = Integer.parseInt(portStr.trim());
                if (port > 0 && port < 65536) {
                    logger.info("Using port from SERVER_PORT property: {}", port);
                    return port;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid SERVER_PORT property value: {}, using default", portStr);
            }
        }

        // 2. Check Environment Variable
        portStr = System.getenv("SERVER_PORT");
        if (portStr != null && !portStr.isBlank()) {
            try {
                int port = Integer.parseInt(portStr.trim());
                if (port > 0 && port < 65536) {
                    logger.info("Using port from SERVER_PORT env: {}", port);
                    return port;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid SERVER_PORT value: {}, using default", portStr);
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

            startProfileReminderTask();

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

    private void startProfileReminderTask() {
        logger.info("Starting profile reminder scheduler");

        UserRepository userRepository = new UserRepository();
        NotificationRepository notificationRepository = new NotificationRepository();
        VacancyRepository vacancyRepository = new VacancyRepository();
        CompanyRepository companyRepository = new CompanyRepository();

        NotificationService notificationService = new NotificationService(
                notificationRepository,
                vacancyRepository,
                companyRepository,
                SessionManager.getInstance());

        ProfileReminderTask profileReminderTask = new ProfileReminderTask(userRepository, notificationService);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(profileReminderTask, 1, 24, TimeUnit.HOURS);

        logger.info("Profile reminder task scheduled to run every 24 hours");
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