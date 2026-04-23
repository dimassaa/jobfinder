package com.jobapp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.*;
import com.jobapp.server.db.DatabaseConfig;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProfileIntegrationTest {

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
    void testProfileFlow() throws Exception {
        String testEmail = "profiletest" + System.currentTimeMillis() + "@test.com";
        String testPassword = "testPassword123";

        int userId;
        String sessionToken;

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

            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> registerData = (LinkedHashMap<String, Object>) registerResp.getData();
            userId = ((Number) registerData.get("userId")).intValue();

            LoginRequest loginReq = new LoginRequest(testEmail, testPassword);
            Message loginMsg = new Message("REQUEST", "LOGIN", loginReq);
            String loginJson = objectMapper.writeValueAsString(loginMsg);
            out.println(loginJson);

            String loginResponse = in.readLine();
            assertNotNull(loginResponse);
            Response loginResp = objectMapper.readValue(loginResponse, Response.class);
            assertEquals("OK", loginResp.getStatus());

            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> loginData = (LinkedHashMap<String, Object>) loginResp.getData();
            Boolean loginSuccess = (Boolean) loginData.get("success");
            assertTrue(loginSuccess, "Login should succeed");

            Message getProfileMsg = new Message("REQUEST", "GET_PROFILE", null);
            String getProfileJson = objectMapper.writeValueAsString(getProfileMsg);
            out.println(getProfileJson);

            String getProfileResponse = in.readLine();
            assertNotNull(getProfileResponse);
            Response getProfileResp = objectMapper.readValue(getProfileResponse, Response.class);
            assertEquals("OK", getProfileResp.getStatus());

            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> profileData = (LinkedHashMap<String, Object>) getProfileResp.getData();
            String name = (String) profileData.get("name");
            String surname = (String) profileData.get("surname");
            assertEquals("Test", name);
            assertEquals("User", surname);

            ProfileDto updatedProfile = new ProfileDto(
                    "UpdatedName",
                    "UpdatedSurname",
                    "Moscow",
                    "+79001234567",
                    "Additional info text"
            );
            Message updateProfileMsg = new Message("REQUEST", "UPDATE_PROFILE", updatedProfile);
            String updateProfileJson = objectMapper.writeValueAsString(updateProfileMsg);
            out.println(updateProfileJson);

            String updateProfileResponse = in.readLine();
            assertNotNull(updateProfileResponse);
            Response updateProfileResp = objectMapper.readValue(updateProfileResponse, Response.class);
            assertEquals("OK", updateProfileResp.getStatus());

            Message getProfileMsg2 = new Message("REQUEST", "GET_PROFILE", null);
            String getProfileJson2 = objectMapper.writeValueAsString(getProfileMsg2);
            out.println(getProfileJson2);

            String getProfileResponse2 = in.readLine();
            assertNotNull(getProfileResponse2);
            Response getProfileResp2 = objectMapper.readValue(getProfileResponse2, Response.class);
            assertEquals("OK", getProfileResp2.getStatus());

            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> profileData2 = (LinkedHashMap<String, Object>) getProfileResp2.getData();
            assertNotNull(profileData2);
            assertEquals("UpdatedName", profileData2.get("name"));
            assertEquals("UpdatedSurname", profileData2.get("surname"));
            assertEquals("Moscow", profileData2.get("city"));
            assertEquals("+79001234567", profileData2.get("phone"));
            assertEquals("Additional info text", profileData2.get("additionalInfo"));

            Message getAllTagsMsg = new Message("REQUEST", "GET_ALL_TAGS", null);
            String getAllTagsJson = objectMapper.writeValueAsString(getAllTagsMsg);
            out.println(getAllTagsJson);

            String getAllTagsResponse = in.readLine();
            assertNotNull(getAllTagsResponse);
            Response getAllTagsResp = objectMapper.readValue(getAllTagsResponse, Response.class);
            assertEquals("OK", getAllTagsResp.getStatus());

            @SuppressWarnings("unchecked")
            List<LinkedHashMap<String, Object>> allTagsMap = (List<LinkedHashMap<String, Object>>) getAllTagsResp.getData();
            assertNotNull(allTagsMap);
            assertFalse(allTagsMap.isEmpty());

            LinkedHashMap<String, Object> tag1Map = allTagsMap.get(0);
            LinkedHashMap<String, Object> tag2Map = allTagsMap.size() > 1 ? allTagsMap.get(1) : allTagsMap.get(0);
            LinkedHashMap<String, Object> tag3Map = allTagsMap.size() > 2 ? allTagsMap.get(2) : allTagsMap.get(0);

            Integer tag1Id = ((Number) tag1Map.get("id")).intValue();
            Integer tag2Id = ((Number) tag2Map.get("id")).intValue();
            Integer tag3Id = ((Number) tag3Map.get("id")).intValue();

            UserTagsUpdateRequest tagsUpdateReq = new UserTagsUpdateRequest(
                    java.util.Arrays.asList(tag1Id, tag2Id, tag3Id)
            );
            Message updateTagsMsg = new Message("REQUEST", "UPDATE_USER_TAGS", tagsUpdateReq);
            String updateTagsJson = objectMapper.writeValueAsString(updateTagsMsg);
            out.println(updateTagsJson);

            String updateTagsResponse = in.readLine();
            assertNotNull(updateTagsResponse);
            Response updateTagsResp = objectMapper.readValue(updateTagsResponse, Response.class);
            assertEquals("OK", updateTagsResp.getStatus());

            Message getUserTagsMsg = new Message("REQUEST", "GET_USER_TAGS", null);
            String getUserTagsJson = objectMapper.writeValueAsString(getUserTagsMsg);
            out.println(getUserTagsJson);

            String getUserTagsResponse = in.readLine();
            assertNotNull(getUserTagsResponse);
            Response getUserTagsResp = objectMapper.readValue(getUserTagsResponse, Response.class);
            assertEquals("OK", getUserTagsResp.getStatus());

            @SuppressWarnings("unchecked")
            List<LinkedHashMap<String, Object>> userTagsMap = (List<LinkedHashMap<String, Object>>) getUserTagsResp.getData();
            assertNotNull(userTagsMap);
            assertEquals(3, userTagsMap.size());

            boolean foundTag1 = userTagsMap.stream().anyMatch(t -> tag1Id.equals(((Number) t.get("id")).intValue()));
            boolean foundTag2 = userTagsMap.stream().anyMatch(t -> tag2Id.equals(((Number) t.get("id")).intValue()));
            boolean foundTag3 = userTagsMap.stream().anyMatch(t -> tag3Id.equals(((Number) t.get("id")).intValue()));
            assertTrue(foundTag1, "Tag1 should be assigned to user");
            assertTrue(foundTag2, "Tag2 should be assigned to user");
            assertTrue(foundTag3, "Tag3 should be assigned to user");
        }
    }

    @Test
    void testGetProfileWithoutLogin() throws Exception {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Thread.sleep(500);

            Message getProfileMsg = new Message("REQUEST", "GET_PROFILE", null);
            String getProfileJson = objectMapper.writeValueAsString(getProfileMsg);
            out.println(getProfileJson);

            String getProfileResponse = in.readLine();
            assertNotNull(getProfileResponse);
            Response getProfileResp = objectMapper.readValue(getProfileResponse, Response.class);
            assertEquals("ERROR", getProfileResp.getStatus());
        }
    }
}