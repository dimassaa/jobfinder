package com.jobapp.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testMessageSerialization() throws Exception {
        String json = "{\"type\":\"REQUEST\",\"command\":\"LOGIN\",\"data\":{\"email\":\"test@example.com\"}}";

        Message message = objectMapper.readValue(json, Message.class);

        assertEquals("REQUEST", message.getType());
        assertEquals("LOGIN", message.getCommand());
        assertNotNull(message.getData());
    }

    @Test
    void testMessageDeserialization() throws Exception {
        Map<String, String> loginData = Map.of("email", "user@test.com", "password", "password123");
        Message message = new Message("REQUEST", "LOGIN", loginData);

        String json = objectMapper.writeValueAsString(message);

        assertTrue(json.contains("REQUEST"));
        assertTrue(json.contains("LOGIN"));
        assertTrue(json.contains("user@test.com"));
    }

    @Test
    void testResponseSerialization() throws Exception {
        String json = "{\"status\":\"OK\",\"message\":\"Login successful\",\"data\":{\"userId\":1}}";

        Response response = objectMapper.readValue(json, Response.class);

        assertEquals("OK", response.getStatus());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void testResponseDeserialization() throws Exception {
        Response response = new Response("ERROR", "Invalid credentials", null);

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("ERROR"));
        assertTrue(json.contains("Invalid credentials"));
    }
}