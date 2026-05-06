package com.jobapp.server.service;

import com.jobapp.common.dto.AuthResponse;
import com.jobapp.common.dto.LoginRequest;
import com.jobapp.common.dto.RegisterRequest;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public AuthService(UserRepository userRepository, CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            logger.warn("Registration attempt with existing email: {}", req.getEmail());
            return new AuthResponse(false, "Email already registered", null, null);
        }

        String hashedPassword = PasswordUtil.hash(req.getPassword());

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(hashedPassword);
        user.setRole(req.getRole() != null ? req.getRole() : "USER");
        user.setName(req.getName());
        user.setSurname(req.getSurname());

        User savedUser = userRepository.save(user);

        if ("HR".equalsIgnoreCase(savedUser.getRole())) {
            Company company = new Company();
            company.setUserId(savedUser.getId());
            company.setCompanyName(savedUser.getName() + "'s Company");
            companyRepository.save(company);
            logger.info("Company created for HR user: {}", savedUser.getId());
        }

        logger.info("User registered successfully: {}", savedUser.getEmail());
        return new AuthResponse(true, "Registration successful", savedUser.getId(), savedUser.getRole());
    }

    public AuthResponse login(LoginRequest req) {
        var userOpt = userRepository.findByEmail(req.getEmail());

        if (userOpt.isEmpty()) {
            logger.warn("Login attempt with non-existent email: {}", req.getEmail());
            return new AuthResponse(false, "Invalid email or password", null, null);
        }

        User user = userOpt.get();

        if (!PasswordUtil.verify(req.getPassword(), user.getPasswordHash())) {
            logger.warn("Failed login attempt for user: {}", req.getEmail());
            return new AuthResponse(false, "Invalid email or password", null, null);
        }

        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            if (user.getBlockedUntil() != null && user.getBlockedUntil().isAfter(LocalDateTime.now())) {
                logger.warn("Login attempt for blocked user: {}", req.getEmail());
                return new AuthResponse(false, "Account is blocked until " + user.getBlockedUntil(), null, null);
            } else if (user.getBlockedUntil() != null && user.getBlockedUntil().isBefore(LocalDateTime.now())) {
                userRepository.updateBlockedStatus(user.getId(), false, null);
                user.setIsBlocked(false);
                user.setBlockedUntil(null);
                logger.info("Auto-unblocked user: {}", req.getEmail());
            }
        }

        logger.info("User logged in successfully: {}", req.getEmail());
        return new AuthResponse(true, "Login successful", user.getId(), user.getRole());
    }
}