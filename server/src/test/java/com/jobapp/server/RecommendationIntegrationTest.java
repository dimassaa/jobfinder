package com.jobapp.server;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.*;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationIntegrationTest extends BaseIntegrationTest {

    private Socket userSocket;
    private PrintWriter userOut;
    private BufferedReader userIn;
    private Socket hrSocket;
    private PrintWriter hrOut;
    private BufferedReader hrIn;

    @AfterEach
    void cleanup() throws Exception {
        if (userSocket != null && !userSocket.isClosed()) userSocket.close();
        if (hrSocket != null && !hrSocket.isClosed()) hrSocket.close();
    }

    private void connectUser() throws Exception {
        userSocket = new Socket("localhost", testPort);
        userOut = new PrintWriter(userSocket.getOutputStream(), true);
        userIn = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
        Thread.sleep(300);
    }

    private void connectHr() throws Exception {
        hrSocket = new Socket("localhost", testPort);
        hrOut = new PrintWriter(hrSocket.getOutputStream(), true);
        hrIn = new BufferedReader(new InputStreamReader(hrSocket.getInputStream()));
        Thread.sleep(1000);
    }

private Response userMessage(String command, Object data) throws Exception {
        Message msg = new Message("REQUEST", command, data);
        userOut.println(objectMapper.writeValueAsString(msg));
        userOut.flush();
        Thread.sleep(500);
        String response;
        int attempts = 0;
        while ((response = userIn.readLine()) != null && attempts < 100) {
            if (response.isEmpty()) {
                attempts++;
                continue;
            }
            if (response.contains("\"type\":\"NOTIFICATION\"")) {
                attempts++;
                continue;
            }
            try {
                Response resp = objectMapper.readValue(response, Response.class);
                return resp;
            } catch (Exception e) {
                attempts++;
                continue;
            }
        }
        throw new RuntimeException("No response received for command: " + command);
    }

    private Response hrMessage(String command, Object data) throws Exception {
        Message msg = new Message("REQUEST", command, data);
        hrOut.println(objectMapper.writeValueAsString(msg));
        hrOut.flush();
        Thread.sleep(500);
        String response = hrIn.readLine();
        while (response != null && response.contains("\"type\":\"NOTIFICATION\"")) {
            response = hrIn.readLine();
        }
        if (response == null || response.isEmpty()) {
            throw new RuntimeException("No response received for command: " + command);
        }
        return objectMapper.readValue(response, Response.class);
    }

@Test
    void testRecommendationCache() throws Exception {
        String cacheUserEmail = "cacheuser" + System.currentTimeMillis() + "@test.com";
        String cacheUserPassword = "cachepass123";

        connectUser();
        userMessage("REGISTER", new RegisterRequest(cacheUserEmail, cacheUserPassword, "USER", "CacheUser", "User"));
        userMessage("LOGIN", new LoginRequest(cacheUserEmail, cacheUserPassword));

        Response tagsResp = userMessage("GET_ALL_TAGS", null);
        assertEquals("OK", tagsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> allTags = (List<LinkedHashMap<String, Object>>) tagsResp.getData();
        int tagId = ((Number) allTags.get(0).get("id")).intValue();

        UserTagsUpdateRequest userTagsReq = new UserTagsUpdateRequest();
        userTagsReq.setTagIds(Arrays.asList(tagId));
        userMessage("UPDATE_USER_TAGS", userTagsReq);

        connectHr();
        String hrCacheEmail = "hrcache" + System.currentTimeMillis() + "@test.com";
        String hrCachePassword = "hrcachepass123";
        hrMessage("REGISTER", new RegisterRequest(hrCacheEmail, hrCachePassword, "HR", "HR", "Company"));
        hrMessage("LOGIN", new LoginRequest(hrCacheEmail, hrCachePassword));

        VacancyCreateRequest vacancyReq = new VacancyCreateRequest(
            "Cached Vacancy",
            "Description",
            "Минск",
            1500,
            2500,
            "FULL_TIME",
            Arrays.asList(tagId)
        );
        hrMessage("CREATE_VACANCY", vacancyReq);

        Thread.sleep(300);

        Response rec1Resp = userMessage("GET_RECOMMENDATIONS", null);
        assertEquals("OK", rec1Resp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> rec1 = (List<LinkedHashMap<String, Object>>) rec1Resp.getData();
        assertTrue(rec1.size() >= 1);

        Thread.sleep(50);

        Response rec2Resp = userMessage("GET_RECOMMENDATIONS", null);
        assertEquals("OK", rec2Resp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> rec2 = (List<LinkedHashMap<String, Object>>) rec2Resp.getData();

        assertEquals(rec1.size(), rec2.size(), "Second call should return cached results");
    }
}