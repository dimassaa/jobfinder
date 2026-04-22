package com.jobapp.server.service;

import com.jobapp.common.dto.AuthResponse;
import com.jobapp.common.dto.LoginRequest;
import com.jobapp.common.dto.RegisterRequest;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private CompanyRepository companyRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        companyRepository = mock(CompanyRepository.class);
        authService = new AuthService(userRepository, companyRepository);
    }

    @Test
    void register_shouldFailWhenEmailExists() {
        RegisterRequest req = new RegisterRequest("test@test.com", "password", "USER", "John", "Doe");
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        AuthResponse response = authService.register(req);

        assertFalse(response.isSuccess());
        assertEquals("Email already registered", response.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldCreateUserWithHashedPassword() {
        RegisterRequest req = new RegisterRequest("test@test.com", "plainpassword", "USER", "John", "Doe");
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setEmail("test@test.com");
        savedUser.setRole("USER");
        savedUser.setName("John");
        savedUser.setSurname("Doe");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(req);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getUserId());
        assertEquals("USER", response.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertNotEquals("plainpassword", saved.getPasswordHash());
        assertTrue(PasswordUtil.verify("plainpassword", saved.getPasswordHash()));
    }

    @Test
    void register_shouldCreateCompanyForHRRole() {
        RegisterRequest req = new RegisterRequest("hr@test.com", "password", "HR", "Jane", "Smith");
        when(userRepository.existsByEmail("hr@test.com")).thenReturn(false);

        User savedUser = new User();
        savedUser.setId(2);
        savedUser.setEmail("hr@test.com");
        savedUser.setRole("HR");
        savedUser.setName("Jane");
        savedUser.setSurname("Smith");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(req);

        assertTrue(response.isSuccess());
        assertEquals("HR", response.getRole());

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        Company company = companyCaptor.getValue();
        assertEquals(2, company.getUserId());
    }

    @Test
    void login_shouldFailWhenUserNotFound() {
        LoginRequest req = new LoginRequest("notfound@test.com", "password");
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        AuthResponse response = authService.login(req);

        assertFalse(response.isSuccess());
        assertEquals("Invalid email or password", response.getMessage());
    }

    @Test
    void login_shouldFailWhenPasswordIncorrect() {
        String realHash = PasswordUtil.hash("correctpassword");
        LoginRequest req = new LoginRequest("test@test.com", "wrongpassword");
        User user = new User();
        user.setId(1);
        user.setEmail("test@test.com");
        user.setPasswordHash(realHash);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(req);

        assertFalse(response.isSuccess());
        assertEquals("Invalid email or password", response.getMessage());
    }

    @Test
    void login_shouldFailWhenUserBlocked() {
        String realHash = PasswordUtil.hash("correctpassword");
        LoginRequest req = new LoginRequest("test@test.com", "correctpassword");
        User user = new User();
        user.setId(1);
        user.setEmail("test@test.com");
        user.setPasswordHash(realHash);
        user.setIsBlocked(true);
        user.setBlockedUntil(LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(req);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("blocked"));
    }

    @Test
    void login_shouldSucceedWithValidCredentials() {
        String realHash = PasswordUtil.hash("correctpassword");
        LoginRequest req = new LoginRequest("test@test.com", "correctpassword");
        User user = new User();
        user.setId(1);
        user.setEmail("test@test.com");
        user.setPasswordHash(realHash);
        user.setRole("USER");
        user.setIsBlocked(false);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(req);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getUserId());
        assertEquals("USER", response.getRole());
    }

    @Test
    void login_shouldSucceedWhenBlockExpired() {
        String realHash = PasswordUtil.hash("correctpassword");
        LoginRequest req = new LoginRequest("test@test.com", "correctpassword");
        User user = new User();
        user.setId(1);
        user.setEmail("test@test.com");
        user.setPasswordHash(realHash);
        user.setRole("USER");
        user.setIsBlocked(true);
        user.setBlockedUntil(LocalDateTime.now().minusHours(1));
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(req);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getUserId());
    }
}