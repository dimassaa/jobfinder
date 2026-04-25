package com.jobapp.server;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.AuthResponse;
import com.jobapp.common.dto.LoginRequest;
import com.jobapp.common.dto.RegisterRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void testRegisterLoginLogoutFlow() throws Exception {
        String testEmail = "integrationtest" + System.currentTimeMillis() + "@test.com";
        String testPassword = "testPassword123";

        Message registerMsg = new Message("REQUEST", "REGISTER",
            new RegisterRequest(testEmail, testPassword, "USER", "Test", "User"));
        out.println(objectMapper.writeValueAsString(registerMsg));

        String registerResponse = in.readLine();
        assertNotNull(registerResponse);
        Response registerResp = objectMapper.readValue(registerResponse, Response.class);
        assertEquals("OK", registerResp.getStatus());
        assertNotNull(registerResp.getData());

        if (registerResp.getData() instanceof AuthResponse authResp) {
            assertTrue(authResp.isSuccess());
            assertNotNull(authResp.getUserId());
            assertEquals("USER", authResp.getRole());
        }

        Message pingMsg = new Message("REQUEST", "PING", null);
        out.println(objectMapper.writeValueAsString(pingMsg));
        String pingResponse = in.readLine();
        assertNotNull(pingResponse);
        Response pingResp = objectMapper.readValue(pingResponse, Response.class);
        assertEquals("OK", pingResp.getStatus());
        assertEquals("PONG", pingResp.getData());

        Message loginMsg = new Message("REQUEST", "LOGIN", new LoginRequest(testEmail, testPassword));
        out.println(objectMapper.writeValueAsString(loginMsg));

        String loginResponse = in.readLine();
        assertNotNull(loginResponse);
        Response loginResp = objectMapper.readValue(loginResponse, Response.class);
        assertEquals("OK", loginResp.getStatus());
        assertNotNull(loginResp.getData());

        if (loginResp.getData() instanceof AuthResponse authResp) {
            assertTrue(authResp.isSuccess());
            assertNotNull(authResp.getUserId());
            assertEquals("USER", authResp.getRole());
        }

        Message logoutMsg = new Message("REQUEST", "LOGOUT", null);
        out.println(objectMapper.writeValueAsString(logoutMsg));

        String logoutResponse = in.readLine();
        assertNotNull(logoutResponse);
        Response logoutResp = objectMapper.readValue(logoutResponse, Response.class);
        assertEquals("OK", logoutResp.getStatus());
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        Message loginMsg = new Message("REQUEST", "LOGIN",
            new LoginRequest("nonexistent@test.com", "wrongpassword"));
        out.println(objectMapper.writeValueAsString(loginMsg));

        String loginResponse = in.readLine();
        assertNotNull(loginResponse);
        Response loginResp = objectMapper.readValue(loginResponse, Response.class);
        assertEquals("ERROR", loginResp.getStatus());

        if (loginResp.getData() instanceof AuthResponse authResp) {
            assertFalse(authResp.isSuccess());
        }
    }
}