package com.jobapp.server.service;

import com.jobapp.server.model.PasswordResetToken;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.PasswordResetTokenRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private EmailService emailService;
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        emailService = mock(EmailService.class);
        passwordResetService = new PasswordResetService(userRepository, tokenRepository, emailService);
    }

    @Test
    void requestPasswordReset_shouldSendEmailForExistingUser() {
        String email = "test@test.com";
        User user = new User();
        user.setId(1);
        user.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        passwordResetService.requestPasswordReset(email);

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).send(eq(email), eq("Восстановление пароля"), contains("токен"));
    }

    @Test
    void requestPasswordReset_shouldNotThrowForNonExistentEmail() {
        String email = "notfound@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.requestPasswordReset(email));
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).send(any(), any(), any());
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndDeleteToken() {
        String token = "valid-token";
        String newPassword = "newPassword123";
        User user = new User();
        user.setId(1);
        user.setEmail("test@test.com");

        PasswordResetToken resetToken = new PasswordResetToken(user.getId(), token, LocalDateTime.now().plusHours(24));
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        passwordResetService.resetPassword(token, newPassword);

        verify(userRepository).updatePassword(eq(user.getId()), argThat(hash ->
            PasswordUtil.verify(newPassword, hash)
        ));
        verify(tokenRepository).deleteByToken(token);
    }

    @Test
    void resetPassword_shouldThrowForInvalidToken() {
        String token = "invalid-token";
        when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            passwordResetService.resetPassword(token, "newPassword")
        );
    }

    @Test
    void resetPassword_shouldThrowForExpiredToken() {
        String token = "expired-token";
        PasswordResetToken expiredToken = new PasswordResetToken(1, token, LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(expiredToken));

        assertThrows(IllegalArgumentException.class, () ->
            passwordResetService.resetPassword(token, "newPassword")
        );
    }

    @Test
    void resetPassword_shouldThrowForNonExistentUser() {
        String token = "valid-token";
        PasswordResetToken resetToken = new PasswordResetToken(999, token, LocalDateTime.now().plusHours(24));
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            passwordResetService.resetPassword(token, "newPassword")
        );
    }
}