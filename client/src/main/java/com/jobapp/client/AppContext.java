package com.jobapp.client;

public class AppContext {

    private static int currentUserId;
    private static String currentRole;
    private static String currentEmail;

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
}