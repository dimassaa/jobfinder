package com.jobapp.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testRegisterRequestSerialization() throws Exception {
        String json = "{\"email\":\"test@example.com\",\"password\":\"pass123\",\"role\":\"USER\",\"name\":\"John\",\"surname\":\"Doe\"}";

        RegisterRequest request = objectMapper.readValue(json, RegisterRequest.class);

        assertEquals("test@example.com", request.getEmail());
        assertEquals("pass123", request.getPassword());
        assertEquals("USER", request.getRole());
        assertEquals("John", request.getName());
        assertEquals("Doe", request.getSurname());
    }

    @Test
    void testRegisterRequestDeserialization() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "pass123", "HR", "Jane", "Smith");

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("test@example.com"));
        assertTrue(json.contains("pass123"));
        assertTrue(json.contains("HR"));
    }

    @Test
    void testLoginRequestSerialization() throws Exception {
        String json = "{\"email\":\"user@test.com\",\"password\":\"secret\"}";

        LoginRequest request = objectMapper.readValue(json, LoginRequest.class);

        assertEquals("user@test.com", request.getEmail());
        assertEquals("secret", request.getPassword());
    }

    @Test
    void testLoginRequestDeserialization() throws Exception {
        LoginRequest request = new LoginRequest("admin@test.com", "admin123");

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("admin@test.com"));
        assertTrue(json.contains("admin123"));
    }

    @Test
    void testAuthResponseSerialization() throws Exception {
        String json = "{\"success\":true,\"message\":\"Login successful\",\"userId\":1,\"role\":\"USER\"}";

        AuthResponse response = objectMapper.readValue(json, AuthResponse.class);

        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals(1, response.getUserId());
        assertEquals("USER", response.getRole());
    }

    @Test
    void testAuthResponseDeserialization() throws Exception {
        AuthResponse response = new AuthResponse(true, "Registered successfully", 5, "HR");

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("true"));
        assertTrue(json.contains("Registered successfully"));
        assertTrue(json.contains("5"));
        assertTrue(json.contains("HR"));
    }
}