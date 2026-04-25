package com.jobapp.server.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 100;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_TEXT_LENGTH = 5000;
    private static final int MIN_VACANCY_SALARY = 0;
    private static final int MAX_VACANCY_SALARY = 100000;

    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Invalid email format";
        }
        if (email.length() > MAX_NAME_LENGTH) {
            return "Email too long";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return "Password too long";
        }
        return null;
    }

    public static String validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            return fieldName + " is required";
        }
        if (name.length() < MIN_NAME_LENGTH) {
            return fieldName + " must be at least " + MIN_NAME_LENGTH + " characters";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return fieldName + " too long";
        }
        return null;
    }

    public static String validateOptionalName(String name, String fieldName) {
        if (name != null && !name.trim().isEmpty()) {
            if (name.length() > MAX_NAME_LENGTH) {
                return fieldName + " too long";
            }
        }
        return null;
    }

    public static String validateText(String text, String fieldName, boolean required) {
        if (required && (text == null || text.trim().isEmpty())) {
            return fieldName + " is required";
        }
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            return fieldName + " too long (max " + MAX_TEXT_LENGTH + " characters)";
        }
        return null;
    }

    public static String validateSalary(Integer salary) {
        if (salary == null) {
            return null;
        }
        if (salary < MIN_VACANCY_SALARY) {
            return "Salary cannot be negative";
        }
        if (salary > MAX_VACANCY_SALARY) {
            return "Salary too high (max " + MAX_VACANCY_SALARY + ")";
        }
        return null;
    }

    public static String validateId(Integer id, String fieldName) {
        if (id == null) {
            return fieldName + " is required";
        }
        if (id <= 0) {
            return "Invalid " + fieldName;
        }
        return null;
    }

    public static String validateRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "Role is required";
        }
        String normalizedRole = role.trim().toUpperCase();
        if (!"JOBSEEKER".equals(normalizedRole) && !"HR".equals(normalizedRole) && !"ADMIN".equals(normalizedRole)) {
            return "Invalid role. Must be JOBSEEKER, HR, or ADMIN";
        }
        return null;
    }

    public static String validateTagName(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            return "Tag name is required";
        }
        if (tagName.length() > MAX_NAME_LENGTH) {
            return "Tag name too long";
        }
        return null;
    }
}