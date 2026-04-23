package com.jobapp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.db.DatabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

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

    @BeforeEach
    void setUpBase() throws Exception {
        objectMapper = new ObjectMapper();
        testPort = findAvailablePort();

        DatabaseConfig.reset();

        Server server = new Server(testPort);
        serverThread = new Thread(() -> server.start());
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(1000);

        socket = new Socket("localhost", testPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        Thread.sleep(500);
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
}