package com.jobapp.server.service;

import com.jobapp.server.model.PasswordResetToken;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.PasswordResetTokenRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_EXPIRY_HOURS = 24;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            logger.debug("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();
        if (tokenRepository.hasActiveToken(user.getId())) {
            logger.debug("Active reset token already exists for user: {}", email);
            return;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        PasswordResetToken resetToken = new PasswordResetToken(user.getId(), token, expiresAt);
        tokenRepository.save(resetToken);

        String emailBody = String.format(
            "Для восстановления пароля используйте токен: %s%n%nСсылка: http://localhost:8081/reset?token=%s%n%nТокен действителен %d часа.",
            token, token, TOKEN_EXPIRY_HOURS
        );

        emailService.send(email, "Восстановление пароля", emailBody);
        logger.info("Password reset token sent to: {}", email);
    }

    public void resetPassword(String token, String newPassword) {
        if (token != null && !token.trim().isEmpty()) {
            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

            if (tokenOpt.isEmpty()) {
                logger.warn("Password reset attempted with invalid token");
                throw new IllegalArgumentException("Invalid or expired token");
            }

            PasswordResetToken resetToken = tokenOpt.get();

            if (resetToken.isExpired()) {
                logger.warn("Password reset attempted with expired token");
                throw new IllegalArgumentException("Invalid or expired token");
            }

            Optional<User> userOpt = userRepository.findById(resetToken.getUserId());
            if (userOpt.isEmpty()) {
                logger.error("User not found for password reset token");
                throw new IllegalArgumentException("User not found");
            }

            User user = userOpt.get();
            String hashedPassword = PasswordUtil.hash(newPassword);
            userRepository.updatePassword(user.getId(), hashedPassword);

            boolean deleted = tokenRepository.deleteByTokenIfValid(token);
            if (!deleted) {
                logger.warn("Token was already used or expired: {}", token);
            }
            tokenRepository.deleteByUserId(user.getId());
            logger.info("Password reset completed for user: {}", user.getId());
        } else {
            logger.warn("Password reset without token - blocked for security");
            throw new IllegalArgumentException("Token is required");
        }
    }

    public void resetPasswordByEmail(String email, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            logger.warn("Password reset by email for non-existent email: {}", email);
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        String hashedPassword = PasswordUtil.hash(newPassword);
        userRepository.updatePassword(user.getId(), hashedPassword);

        logger.info("Password reset by email completed for user: {}", user.getEmail());
    }
}