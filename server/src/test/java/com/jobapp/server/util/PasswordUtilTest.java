package com.jobapp.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hash_shouldReturnNonNullHash() {
        String hash = PasswordUtil.hash("password123");
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    @Test
    void hash_shouldReturnDifferentValueFromPlainText() {
        String plainPassword = "password123";
        String hash = PasswordUtil.hash(plainPassword);
        assertNotEquals(plainPassword, hash);
    }

    @Test
    void verify_shouldReturnTrueForCorrectPassword() {
        String plainPassword = "password123";
        String hash = PasswordUtil.hash(plainPassword);

        assertTrue(PasswordUtil.verify(plainPassword, hash));
    }

    @Test
    void verify_shouldReturnFalseForIncorrectPassword() {
        String plainPassword = "password123";
        String wrongPassword = "wrongpassword";
        String hash = PasswordUtil.hash(plainPassword);

        assertFalse(PasswordUtil.verify(wrongPassword, hash));
    }

    @Test
    void hash_shouldGenerateUniqueHashes() {
        String plainPassword = "password123";
        String hash1 = PasswordUtil.hash(plainPassword);
        String hash2 = PasswordUtil.hash(plainPassword);

        assertNotEquals(hash1, hash2);
        assertTrue(PasswordUtil.verify(plainPassword, hash1));
assertTrue(PasswordUtil.verify(plainPassword, hash2));
    }
}