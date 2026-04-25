package com.jobapp.server.util;

import com.jobapp.server.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityValidatorTest {

    @Test
    void testRequiresAuth_true() {
        assertTrue(SecurityValidator.requiresAuth("GET_PROFILE"));
        assertTrue(SecurityValidator.requiresAuth("UPDATE_PROFILE"));
        assertTrue(SecurityValidator.requiresAuth("APPLY"));
    }

    @Test
    void testRequiresAuth_false() {
        assertFalse(SecurityValidator.requiresAuth("LOGIN"));
        assertFalse(SecurityValidator.requiresAuth("REGISTER"));
        assertFalse(SecurityValidator.requiresAuth("SEARCH_VACANCIES"));
        assertFalse(SecurityValidator.requiresAuth(null));
    }

    @Test
    void testRequiresAdmin_true() {
        assertTrue(SecurityValidator.requiresAdmin("GET_ALL_USERS"));
        assertTrue(SecurityValidator.requiresAdmin("BLOCK_USER"));
        assertTrue(SecurityValidator.requiresAdmin("DELETE_USER"));
    }

    @Test
    void testRequiresAdmin_false() {
        assertFalse(SecurityValidator.requiresAdmin("GET_PROFILE"));
        assertFalse(SecurityValidator.requiresAdmin("LOGIN"));
    }

    @Test
    void testRequiresHR_true() {
        assertTrue(SecurityValidator.requiresHR("CREATE_VACANCY"));
        assertTrue(SecurityValidator.requiresHR("UPDATE_VACANCY"));
    }

    @Test
    void testAllowsGuest_true() {
        assertTrue(SecurityValidator.allowsGuest("LOGIN"));
        assertTrue(SecurityValidator.allowsGuest("REGISTER"));
        assertTrue(SecurityValidator.allowsGuest("GET_ALL_TAGS"));
        assertTrue(SecurityValidator.allowsGuest("GET_VACANCY_DETAILS"));
    }

    @Test
    void testCheckRole_admin() {
        User admin = new User();
        admin.setRole("ADMIN");
        assertTrue(SecurityValidator.checkRole(admin, "ADMIN"));
    }

    @Test
    void testCheckRole_adminFalse() {
        User jobseeker = new User();
        jobseeker.setRole("JOBSEEKER");
        assertFalse(SecurityValidator.checkRole(jobseeker, "ADMIN"));
    }

    @Test
    void testCheckRole_nullUser() {
        assertFalse(SecurityValidator.checkRole(null, "ADMIN"));
    }

    @Test
    void testIsUserBlocked_blockedUser() {
        User user = new User();
        user.setIsBlocked(true);
        user.setBlockedUntil(java.time.LocalDateTime.now().plusHours(1));
        assertTrue(SecurityValidator.isUserBlocked(user));
    }

    @Test
    void testIsUserBlocked_expiredBlock() {
        User user = new User();
        user.setIsBlocked(true);
        user.setBlockedUntil(java.time.LocalDateTime.now().minusHours(1));
        assertFalse(SecurityValidator.isUserBlocked(user));
    }

    @Test
    void testIsUserBlocked_notBlocked() {
        User user = new User();
        user.setIsBlocked(false);
        assertFalse(SecurityValidator.isUserBlocked(user));
    }

    @Test
    void testIsUserBlocked_nullUser() {
        assertTrue(SecurityValidator.isUserBlocked(null));
    }
}