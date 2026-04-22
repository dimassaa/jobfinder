package com.jobapp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.AuthResponse;
import com.jobapp.common.dto.LoginRequest;
import com.jobapp.common.dto.RegisterRequest;
import com.jobapp.server.db.DatabaseConfig;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTest {

    private static final int TEST_PORT = findRandomPort();
    private static Server server;
    private static Thread serverThread;
    private ObjectMapper objectMapper;

    private static int findRandomPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void setUpServer() throws Exception {
        System.setProperty("DB_HOST", "localhost");
        System.setProperty("DB_PORT", "5432");
        System.setProperty("DB_NAME", "jobdb");
        System.setProperty("DB_USER", "jobuser");
        System.setProperty("DB_PASSWORD", "secret");

        try {
            DatabaseConfig.getDataSource().getConnection();
        } catch (Exception e) {
            DatabaseConfig.close();
        }

        serverThread = new Thread(() -> {
            server = new Server(TEST_PORT);
            server.start();
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(2000);
    }

    @AfterAll
    static void tearDownServer() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testRegisterLoginLogoutFlow() throws Exception {
        String testEmail = "integrationtest" + System.currentTimeMillis() + "@test.com";
        String testPassword = "testPassword123";

        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Thread.sleep(500);

            RegisterRequest registerReq = new RegisterRequest(testEmail, testPassword, "USER", "Test", "User");
            Message registerMsg = new Message("REQUEST", "REGISTER", registerReq);
            String registerJson = objectMapper.writeValueAsString(registerMsg);
            out.println(registerJson);

            String registerResponse = in.readLine();
            assertNotNull(registerResponse);

            Response registerResp = objectMapper.readValue(registerResponse, Response.class);
            assertEquals("OK", registerResp.getStatus());
            assertNotNull(registerResp.getData());

            if (registerResp.getData() instanceof AuthResponse) {
                AuthResponse authResp = (AuthResponse) registerResp.getData();
                assertTrue(authResp.isSuccess(), "Registration should succeed");
                assertNotNull(authResp.getUserId());
                assertEquals("USER", authResp.getRole());
            }

            LoginRequest loginReq = new LoginRequest(testEmail, testPassword);
            Message loginMsg = new Message("REQUEST", "LOGIN", loginReq);
            String loginJson = objectMapper.writeValueAsString(loginMsg);
            out.println(loginJson);

            String loginResponse = in.readLine();
            assertNotNull(loginResponse);

            Response loginResp = objectMapper.readValue(loginResponse, Response.class);
            assertEquals("OK", loginResp.getStatus());
            assertNotNull(loginResp.getData());

            if (loginResp.getData() instanceof AuthResponse) {
                AuthResponse authResp = (AuthResponse) loginResp.getData();
                assertTrue(authResp.isSuccess(), "Login should succeed");
                assertNotNull(authResp.getUserId());
                assertEquals("USER", authResp.getRole());
            }

            Message logoutMsg = new Message("REQUEST", "LOGOUT", null);
            String logoutJson = objectMapper.writeValueAsString(logoutMsg);
            out.println(logoutJson);

            String logoutResponse = in.readLine();
            assertNotNull(logoutResponse);

            Response logoutResp = objectMapper.readValue(logoutResponse, Response.class);
            assertEquals("OK", logoutResp.getStatus());
        }
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Thread.sleep(500);

            LoginRequest loginReq = new LoginRequest("nonexistent@test.com", "wrongpassword");
            Message loginMsg = new Message("REQUEST", "LOGIN", loginReq);
            String loginJson = objectMapper.writeValueAsString(loginMsg);
            out.println(loginJson);

            String loginResponse = in.readLine();
            assertNotNull(loginResponse);

            Response loginResp = objectMapper.readValue(loginResponse, Response.class);
            assertEquals("ERROR", loginResp.getStatus());

            if (loginResp.getData() instanceof AuthResponse) {
                AuthResponse authResp = (AuthResponse) loginResp.getData();
                assertFalse(authResp.isSuccess(), "Login should fail with invalid credentials");
            }
        }
    }

    @Test
    void testPingPong() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Thread.sleep(500);

            Message pingMsg = new Message("REQUEST", "PING", null);
            String pingJson = objectMapper.writeValueAsString(pingMsg);
            out.println(pingJson);

            String response = in.readLine();
            assertNotNull(response);

            Response resp = objectMapper.readValue(response, Response.class);
            assertEquals("OK", resp.getStatus());
            assertEquals("PONG", resp.getData());
        }
    }
}