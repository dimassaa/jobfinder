package com.jobapp.client;

public class AppContext {

    private static int currentUserId;
    private static String currentRole;
    private static String currentEmail;

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
        return currentUserId;
    }

    public static void setCurrentUserId(int userId) {
        currentUserId = userId;
    }

    public static String getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentRole(String role) {
        currentRole = role;
    }

    public static String getCurrentEmail() {
        return currentEmail;
    }

    public static void setCurrentEmail(String email) {
        currentEmail = email;
    }

    public static boolean isLoggedIn() {
        return currentUserId > 0;
    }

    public static boolean isHr() {
        return "HR".equals(currentRole);
    }

    public static void clear() {
        currentUserId = 0;
        currentRole = null;
        currentEmail = null;
    }

    public static String getServerConfigInfo() {
        return String.format("Server: %s:%d", serverHost, serverPort);
    }
}