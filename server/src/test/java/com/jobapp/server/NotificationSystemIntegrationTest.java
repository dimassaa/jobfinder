package com.jobapp.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.AuthResponse;
import com.jobapp.common.dto.NotificationDto;
import com.jobapp.common.dto.UserTagsUpdateRequest;
import com.jobapp.common.dto.VacancyCreateRequest;
import com.jobapp.common.dto.VacancyDto;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationSystemIntegrationTest extends BaseIntegrationTest {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // Helper class to simulate a client
    static class TestClient implements AutoCloseable {
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        private final List<NotificationDto> notifications = new CopyOnWriteArrayList<>();
        private final Thread listener;
        private volatile boolean running = true;
        private Integer userId;

        public TestClient(int port) throws IOException {
            socket = new Socket("localhost", port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            listener = new Thread(() -> {
                try {
                    String line;
                    while (running && (line = in.readLine()) != null) {
                        System.out.println("[CLIENT " + userId + "] Received: " + line);
                        if (line.contains("\"type\":\"NOTIFICATION\"")) {
                            try {
                                int dataStart = line.indexOf("\"data\":");
                                int braceStart = line.indexOf('{', dataStart);
                                if (braceStart > 0) {
                                    int braceCount = 1;
                                    int end = braceStart + 1;
                                    while (end < line.length() && braceCount > 0) {
                                        if (line.charAt(end) == '{') braceCount++;
                                        else if (line.charAt(end) == '}') braceCount--;
                                        end++;
                                    }
                                    String jsonData = line.substring(braceStart, end);
                                    NotificationDto dto = objectMapper.readValue(jsonData, NotificationDto.class);
                                    notifications.add(dto);
                                }
                            } catch (Exception e) {
                                System.err.println("Error parsing notification: " + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    if (running) e.printStackTrace();
                }
            });
            listener.setDaemon(true);
            listener.start();
        }

        public Response sendCommand(String command, Object data) throws Exception {
            Message msg = new Message("REQUEST", command, data);
            String json = objectMapper.writeValueAsString(msg);
            System.out.println("[CLIENT " + userId + "] Sending: " + json);
            out.println(json);

            String line;
            while ((line = in.readLine()) != null) {
                if (!line.contains("\"type\":\"NOTIFICATION\"")) {
                    return objectMapper.readValue(line, Response.class);
                }
            }
            return null;
        }

        public void login(String email, String password) throws Exception {
            Map<String, String> data = new HashMap<>();
            data.put("email", email);
            data.put("password", password);
            Response resp = sendCommand("LOGIN", data);
            assertNotNull(resp);
            assertEquals("OK", resp.getStatus(), "Login failed: " + resp.getMessage());
            if (resp.getData() != null) {
                AuthResponse auth = objectMapper.convertValue(resp.getData(), AuthResponse.class);
                this.userId = auth.getUserId();
            }
        }

        public List<NotificationDto> getReceivedNotifications() {
            return new ArrayList<>(notifications);
        }

        @Override
        public void close() throws Exception {
            running = false;
            if (!socket.isClosed()) socket.close();
            listener.interrupt();
        }
    }

    @Test
    void testFullNotificationFlow() throws Exception {
        String hrEmail = "hr_" + System.currentTimeMillis() + "@test.com";
        String appEmail = "app_" + System.currentTimeMillis() + "@test.com";

        try (TestClient hr = new TestClient(testPort);
             TestClient applicant = new TestClient(testPort)) {

            // 1. Register HR
            Map<String, Object> hrReg = new HashMap<>();
            hrReg.put("email", hrEmail);
            hrReg.put("password", "pass123");
            hrReg.put("role", "HR");
            hrReg.put("name", "Test HR");
            Response resp = hr.sendCommand("REGISTER", hrReg);
            assertNotNull(resp);
            assertEquals("OK", resp.getStatus());

            hr.login(hrEmail, "pass123");

            // 2. Register Applicant
            Map<String, Object> appReg = new HashMap<>();
            appReg.put("email", appEmail);
            appReg.put("password", "pass123");
            appReg.put("role", "USER");
            appReg.put("name", "Test Applicant");
            resp = applicant.sendCommand("REGISTER", appReg);
            assertNotNull(resp);
            assertEquals("OK", resp.getStatus());

            applicant.login(appEmail, "pass123");

            // 3. Applicant adds tags (Tag ID 1 is 'Java' from init.sql)
            UserTagsUpdateRequest tagsRequest = new UserTagsUpdateRequest();
            tagsRequest.setTagIds(List.of(1));
            resp = applicant.sendCommand("UPDATE_USER_TAGS", tagsRequest);
            assertNotNull(resp);
            assertEquals("OK", resp.getStatus());

            // 4. HR Creates Vacancy
            VacancyCreateRequest vacancyRequest = new VacancyCreateRequest();
            vacancyRequest.setTitle("Java Developer");
            vacancyRequest.setDescription("Test vacancy");
            vacancyRequest.setCity("Минск");
            vacancyRequest.setEmploymentType("FULL_TIME");
            vacancyRequest.setTagIds(List.of(1));

            resp = hr.sendCommand("CREATE_VACANCY", vacancyRequest);
            assertNotNull(resp);
            assertEquals("OK", resp.getStatus());

            VacancyDto createdVacancy = objectMapper.convertValue(resp.getData(), VacancyDto.class);
            assertNotNull(createdVacancy);
            System.out.println("Vacancy created: " + createdVacancy.getId());

            // 5. Check Applicant gets Push Notification
            Thread.sleep(300);
            List<NotificationDto> appNotifications = applicant.getReceivedNotifications();
            assertFalse(appNotifications.isEmpty());
            assertEquals("New Matching Vacancy", appNotifications.get(0).getTitle());

            // 6. Applicant Apply
            Map<String, Object> applyData = new HashMap<>();
            applyData.put("vacancyId", createdVacancy.getId());
            resp = applicant.sendCommand("APPLY", applyData);
            assertNotNull(resp);
            assertEquals("OK", resp.getStatus());

            // 7. Check HR gets Notification about application
            Thread.sleep(300);
            List<NotificationDto> hrNotifications = hr.getReceivedNotifications();
            assertFalse(hrNotifications.isEmpty());
            assertEquals("New Application", hrNotifications.get(0).getTitle());

            // 8. HR Updates Application Status
            // First get application id
            Map<String, Object> getAppsData = new HashMap<>();
            getAppsData.put("vacancyId", createdVacancy.getId());
            resp = hr.sendCommand("GET_APPLICATIONS_FOR_VACANCY", getAppsData);
            assertNotNull(resp);
            assertEquals("OK", resp.getStatus());

            List<Object> applications = objectMapper.convertValue(resp.getData(), new TypeReference<List<Object>>() {});
            if (!applications.isEmpty()) {
                Map<String, Object> firstApp = (Map<String, Object>) applications.get(0);
                Integer appId = (Integer) firstApp.get("id");

                Map<String, Object> statusData = new HashMap<>();
                statusData.put("applicationId", appId);
                statusData.put("status", "INVITED");

                resp = hr.sendCommand("UPDATE_APPLICATION_STATUS", statusData);
                assertNotNull(resp);
                assertEquals("OK", resp.getStatus());

                // 9. Check Applicant gets Status Update Notification
                Thread.sleep(300);
                appNotifications = applicant.getReceivedNotifications();
                boolean statusUpdateReceived = appNotifications.stream()
                        .anyMatch(n -> "Application Status Updated".equals(n.getTitle()));
                assertTrue(statusUpdateReceived);
            }

            // 10. Test GET_NOTIFICATIONS
            resp = applicant.sendCommand("GET_NOTIFICATIONS", null);
            assertNotNull(resp);
            assertEquals("OK", resp.getStatus());

            // 11. Test MARK_NOTIFICATION_READ
            if (!applicant.getReceivedNotifications().isEmpty()) {
                Integer notifId = applicant.getReceivedNotifications().get(0).getId();
                Map<String, Object> markData = new HashMap<>();
                markData.put("notificationId", notifId);
                resp = applicant.sendCommand("MARK_NOTIFICATION_READ", markData);
                assertNotNull(resp);
                assertEquals("OK", resp.getStatus());
            }
        }
    }
}
