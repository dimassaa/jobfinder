package com.jobapp.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AppContext {

    private static final AtomicInteger currentUserId = new AtomicInteger(0);
    private static final AtomicReference<String> currentRole = new AtomicReference<>();
    private static final AtomicReference<String> currentEmail = new AtomicReference<>();
    private static volatile NetworkService networkService;

    private static final String DEFAULT_HOST = "localhost";
    private static final int LOCAL_PORT = 8081;
    private static final int DEFAULT_CLIENT_PORT = 8081;

    private static String serverHost;
    private static int serverPort;

    static {
        initServerConfig();
    }

    private static void initServerConfig() {
        String envPort = System.getenv("SERVER_PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                int port = Integer.parseInt(envPort.trim());
                if (port > 0 && port < 65536) {
                    serverHost = DEFAULT_HOST;
                    serverPort = port;
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }
        serverHost = DEFAULT_HOST;
        serverPort = DEFAULT_CLIENT_PORT;
    }

    public static String getServerHost() {
        return serverHost;
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static int getCurrentUserId() {
        return currentUserId.get();
    }

    public static void setCurrentUserId(int userId) {
        currentUserId.set(userId);
    }

    public static String getCurrentRole() {
        return currentRole.get();
    }

    public static void setCurrentRole(String role) {
        currentRole.set(role);
    }

    public static String getCurrentEmail() {
        return currentEmail.get();
    }

    public static void setCurrentEmail(String email) {
        currentEmail.set(email);
    }

    public static void setNetworkService(NetworkService ns) {
        networkService = ns;
    }

    public static NetworkService getNetworkService() {
        return networkService;
    }

    public static boolean isLoggedIn() {
        return currentUserId.get() > 0;
    }

    public static boolean isHr() {
        return "HR".equals(currentRole.get());
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(currentRole.get());
    }

    public static void clear() {
        currentUserId.set(0);
        currentRole.set(null);
        currentEmail.set(null);
    }

    public static String getServerConfigInfo() {
        return String.format("Server: %s:%d", serverHost, serverPort);
    }
}