package com.jobapp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.db.DatabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class BaseIntegrationTest {

    protected ObjectMapper objectMapper;
    protected Socket socket;
    protected PrintWriter out;
    protected BufferedReader in;
    protected int testPort;
    private Thread serverThread;

    static {
        System.setProperty("DB_HOST", "localhost");
        System.setProperty("DB_PORT", "5432");
        System.setProperty("DB_NAME", "jobdb");
        System.setProperty("DB_USER", "jobuser");
        System.setProperty("DB_PASSWORD", "secret");
    }

    protected static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForServer(int port, int timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try (Socket testSocket = new Socket("localhost", port)) {
                return;
            } catch (IOException e) {
                Thread.sleep(50);
            }
        }
        throw new RuntimeException("Server did not start within " + timeoutMs + "ms");
    }

    @BeforeEach
    void setUpBase() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testPort = findAvailablePort();

        DatabaseConfig.reset();

        Server server = new Server(testPort);
        serverThread = new Thread(() -> server.start());
        serverThread.start();

        waitForServer(testPort, 5000);

        socket = new Socket("localhost", testPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @AfterEach
    void tearDownBase() throws Exception {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread.join(2000);
        }
    }

    protected Response message(String command, Object data) throws Exception {
        Message msg = new Message("REQUEST", command, data);
        out.println(objectMapper.writeValueAsString(msg));
        String response = in.readLine();
        assertNotNull(response);
        return objectMapper.readValue(response, Response.class);
    }
}