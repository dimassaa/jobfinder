package com.jobapp.server.util;

import com.jobapp.server.model.User;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.service.SessionManager;

import java.util.Set;

public class SecurityValidator {

    private static final Set<String> ADMIN_COMMANDS = Set.of(
            "GET_ALL_USERS", "BLOCK_USER", "UNBLOCK_USER", "DELETE_USER",
            "GET_ALL_VACANCIES_ADMIN", "DELETE_VACANCY_ADMIN",
            "GET_COMPLAINTS_ADMIN", "RESOLVE_COMPLAINT",
            "ADD_TAG", "DEACTIVATE_TAG", "CREATE_ADMIN"
    );

    private static final Set<String> HR_COMMANDS = Set.of(
            "CREATE_VACANCY", "UPDATE_VACANCY", "CLOSE_VACANCY", "GET_MY_VACANCIES",
            "GET_APPLICATIONS_FOR_VACANCY", "UPDATE_APPLICATION_STATUS",
            "GET_ALL_APPLICATIONS_FOR_HR", "GET_COMPANY_PROFILE", "SAVE_COMPANY_PROFILE",
            "GET_CONVERSION", "GET_SALARY_COMPARISON"
    );

    private static final Set<String> AUTH_COMMANDS = Set.of(
            "GET_PROFILE", "UPDATE_PROFILE", "GET_USER_TAGS", "UPDATE_USER_TAGS",
            "APPLY", "GET_MY_APPLICATIONS", "ADD_FAVORITE", "REMOVE_FAVORITE",
            "GET_FAVORITES", "ADD_VIEW", "GET_RECENT_VIEWS", "CREATE_COMPLAINT",
            "GET_RECOMMENDATIONS", "GET_SALARY_STATS",
            "GET_NOTIFICATIONS", "MARK_NOTIFICATION_READ",
            "DELETE_NOTIFICATION", "GET_UNREAD_COUNT", "GET_ACTIVITY_TIMELINE"
    );

    public static boolean requiresAuth(String command) {
        if (command == null) {
            return false;
        }
        return AUTH_COMMANDS.contains(command.toUpperCase());
    }

    public static boolean requiresAdmin(String command) {
        if (command == null) {
            return false;
        }
        return ADMIN_COMMANDS.contains(command.toUpperCase());
    }

    public static boolean requiresHR(String command) {
        if (command == null) {
            return false;
        }
        return HR_COMMANDS.contains(command.toUpperCase());
    }

    public static boolean allowsGuest(String command) {
        if (command == null) {
            return true;
        }
        String upper = command.toUpperCase();
        return !ADMIN_COMMANDS.contains(upper) && !HR_COMMANDS.contains(upper) && !AUTH_COMMANDS.contains(upper);
    }

    public static boolean checkRole(User user, String requiredRole) {
        if (user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(requiredRole)) {
            return "ADMIN".equalsIgnoreCase(user.getRole());
        }
        if ("HR".equalsIgnoreCase(requiredRole)) {
            return "HR".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole());
        }
        return user.getId() != null;
    }

    public static boolean isUserBlocked(User user) {
        if (user == null) {
            return true;
        }
        if (user.getIsBlocked() != null && user.getIsBlocked()) {
            if (user.getBlockedUntil() != null && user.getBlockedUntil().isAfter(java.time.LocalDateTime.now())) {
                return true;
            }
        }
        return false;
    }
}