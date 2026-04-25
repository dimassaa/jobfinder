package com.jobapp.server.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void testValidateEmail_success() {
        assertNull(ValidationUtil.validateEmail("user@example.com"));
    }

    @Test
    void testValidateEmail_null() {
        assertEquals("Email is required", ValidationUtil.validateEmail(null));
    }

    @Test
    void testValidateEmail_empty() {
        assertEquals("Email is required", ValidationUtil.validateEmail(""));
    }

    @Test
    void testValidateEmail_invalid() {
        assertEquals("Invalid email format", ValidationUtil.validateEmail("invalid-email"));
    }

    @Test
    void testValidateEmail_tooLong() {
        String longEmail = "a".repeat(100) + "@example.com";
        assertEquals("Email too long", ValidationUtil.validateEmail(longEmail));
    }

    @Test
    void testValidatePassword_success() {
        assertNull(ValidationUtil.validatePassword("password123"));
    }

    @Test
    void testValidatePassword_null() {
        assertEquals("Password is required", ValidationUtil.validatePassword(null));
    }

    @Test
    void testValidatePassword_empty() {
        assertEquals("Password is required", ValidationUtil.validatePassword(""));
    }

    @Test
    void testValidatePassword_tooShort() {
        assertEquals("Password must be at least 6 characters", ValidationUtil.validatePassword("12345"));
    }

    @Test
    void testValidateName_success() {
        assertNull(ValidationUtil.validateName("John", "Name"));
    }

    @Test
    void testValidateName_null() {
        assertEquals("Name is required", ValidationUtil.validateName(null, "Name"));
    }

    @Test
    void testValidateName_tooShort() {
        assertEquals("Name must be at least 2 characters", ValidationUtil.validateName("A", "Name"));
    }

    @Test
    void testValidateSalary_success() {
        assertNull(ValidationUtil.validateSalary(1000));
    }

    @Test
    void testValidateSalary_negative() {
        assertEquals("Salary cannot be negative", ValidationUtil.validateSalary(-100));
    }

    @Test
    void testValidateSalary_tooHigh() {
        assertEquals("Salary too high (max 100000)", ValidationUtil.validateSalary(200000));
    }

    @Test
    void testValidateId_success() {
        assertNull(ValidationUtil.validateId(1, "User ID"));
    }

    @Test
    void testValidateId_null() {
        assertEquals("User ID is required", ValidationUtil.validateId(null, "User ID"));
    }

    @Test
    void testValidateId_zero() {
        assertEquals("Invalid User ID", ValidationUtil.validateId(0, "User ID"));
    }

    @Test
    void testValidateRole_success() {
        assertNull(ValidationUtil.validateRole("JOBSEEKER"));
        assertNull(ValidationUtil.validateRole("HR"));
        assertNull(ValidationUtil.validateRole("ADMIN"));
    }

    @Test
    void testValidateRole_null() {
        assertEquals("Role is required", ValidationUtil.validateRole(null));
    }

    @Test
    void testValidateRole_invalid() {
        assertEquals("Invalid role. Must be JOBSEEKER, HR, or ADMIN", ValidationUtil.validateRole("INVALID"));
    }
}