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
        Thread.sleep(300);
    }

    private Response userMessage(String command, Object data) throws Exception {
        Message msg = new Message("REQUEST", command, data);
        userOut.println(objectMapper.writeValueAsString(msg));
        String response = userIn.readLine();
        assertNotNull(response);
        return objectMapper.readValue(response, Response.class);
    }

    private Response hrMessage(String command, Object data) throws Exception {
        Message msg = new Message("REQUEST", command, data);
        hrOut.println(objectMapper.writeValueAsString(msg));
        String response = hrIn.readLine();
        assertNotNull(response);
        return objectMapper.readValue(response, Response.class);
    }

    @Test
    void testRecommendationFlow() throws Exception {
        String hrEmail = "hrrec" + System.currentTimeMillis() + "@test.com";
        String hrPassword = "hrrecpass123";

        connectHr();
        hrMessage("REGISTER", new RegisterRequest(hrEmail, hrPassword, "HR", "HRRec", "Company"));
        hrMessage("LOGIN", new LoginRequest(hrEmail, hrPassword));

        Response tagsResp = hrMessage("GET_ALL_TAGS", null);
        assertEquals("OK", tagsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> allTags = (List<LinkedHashMap<String, Object>>) tagsResp.getData();
        assertNotNull(allTags);
        assertTrue(allTags.size() >= 3, "Need at least 3 tags for this test");

        int javaTagId = ((Number) allTags.get(0).get("id")).intValue();
        int sqlTagId = ((Number) allTags.get(1).get("id")).intValue();
        int pythonTagId = allTags.size() > 2 ? ((Number) allTags.get(2).get("id")).intValue() : javaTagId;

        VacancyCreateRequest vacancyA = new VacancyCreateRequest(
            "Java Developer",
            "Looking for Java developer with Spring",
            "Moscow",
            100000,
            150000,
            "FULL_TIME",
            Arrays.asList(javaTagId)
        );
        Response vacAResp = hrMessage("CREATE_VACANCY", vacancyA);
        assertEquals("OK", vacAResp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> vacancyAMap = (LinkedHashMap<String, Object>) vacAResp.getData();
        int vacancyAId = ((Number) vacancyAMap.get("id")).intValue();

        VacancyCreateRequest vacancyB = new VacancyCreateRequest(
            "Python Developer",
            "Python and Django developer",
            "Moscow",
            80000,
            120000,
            "FULL_TIME",
            Arrays.asList(pythonTagId)
        );
        Response vacBResp = hrMessage("CREATE_VACANCY", vacancyB);
        assertEquals("OK", vacBResp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> vacancyBMap = (LinkedHashMap<String, Object>) vacBResp.getData();
        int vacancyBId = ((Number) vacancyBMap.get("id")).intValue();

        String userEmail = "recuser" + System.currentTimeMillis() + "@test.com";
        String userPassword = "recpass123";

        connectUser();
        userMessage("REGISTER", new RegisterRequest(userEmail, userPassword, "USER", "RecUser", "User"));
        userMessage("LOGIN", new LoginRequest(userEmail, userPassword));

        UserTagsUpdateRequest userTagsReq = new UserTagsUpdateRequest();
        userTagsReq.setTagIds(Arrays.asList(javaTagId, sqlTagId));
        Response updateTagsResp = userMessage("UPDATE_USER_TAGS", userTagsReq);
        assertEquals("OK", updateTagsResp.getStatus());

        Response getRecResp = userMessage("GET_RECOMMENDATIONS", null);
        assertEquals("OK", getRecResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> recommendations = (List<LinkedHashMap<String, Object>>) getRecResp.getData();
        assertNotNull(recommendations);

        boolean foundVacancyA = false;
        boolean foundVacancyB = false;
        double prevPercent = Double.MAX_VALUE;

        for (LinkedHashMap<String, Object> rec : recommendations) {
            int vacancyId = ((Number) rec.get("id")).intValue();
            double matchPercent = ((Number) rec.get("matchPercent")).doubleValue();

            if (vacancyId == vacancyAId) {
                foundVacancyA = true;
                int userTags = 2;
                int vacancyTags = 1;
                int intersection = 1;
                int union = userTags + vacancyTags - intersection;
                double expectedPercent = (intersection * 2.0 / union) * 100;
                assertEquals(expectedPercent, matchPercent, 0.01, "Vacancy A should have " + expectedPercent + "% match");
            }

            if (vacancyId == vacancyBId) {
                foundVacancyB = true;
            }

            assertTrue(matchPercent <= prevPercent, "Recommendations should be sorted by matchPercent descending");
            prevPercent = matchPercent;
        }

        assertTrue(foundVacancyA, "Vacancy A with matching tags should be in recommendations");
        assertFalse(foundVacancyB, "Vacancy B with no matching tags should NOT be in recommendations");
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
            "Moscow",
            50000,
            100000,
            "FULL_TIME",
            Arrays.asList(tagId)
        );
        hrMessage("CREATE_VACANCY", vacancyReq);

        Response rec1Resp = userMessage("GET_RECOMMENDATIONS", null);
        assertEquals("OK", rec1Resp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> rec1 = (List<LinkedHashMap<String, Object>>) rec1Resp.getData();
        assertTrue(rec1.size() >= 1);

        Response rec2Resp = userMessage("GET_RECOMMENDATIONS", null);
        assertEquals("OK", rec2Resp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> rec2 = (List<LinkedHashMap<String, Object>>) rec2Resp.getData();

        assertEquals(rec1.size(), rec2.size(), "Second call should return cached results");
    }
}