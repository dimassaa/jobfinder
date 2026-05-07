package com.jobapp.server;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsIntegrationTest extends BaseIntegrationTest {

    private Socket userSocket;
    private PrintWriter userOut;
    private BufferedReader userIn;
    private Socket hrSocket;
    private PrintWriter hrOut;
    private BufferedReader hrIn;
    private Socket adminSocket;
    private PrintWriter adminOut;
    private BufferedReader adminIn;

    @BeforeEach
    @Override
    void setUpBase() throws Exception {
        super.setUpBase();
        userSocket = new Socket("localhost", testPort);
        userOut = new PrintWriter(userSocket.getOutputStream(), true);
        userIn = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
        Thread.sleep(300);

        hrSocket = new Socket("localhost", testPort);
        hrOut = new PrintWriter(hrSocket.getOutputStream(), true);
        hrIn = new BufferedReader(new InputStreamReader(hrSocket.getInputStream()));
        Thread.sleep(300);

        adminSocket = new Socket("localhost", testPort);
        adminOut = new PrintWriter(adminSocket.getOutputStream(), true);
        adminIn = new BufferedReader(new InputStreamReader(adminSocket.getInputStream()));
        Thread.sleep(300);
    }

    @AfterEach
    @Override
    protected void tearDownBase() throws Exception {
        if (userSocket != null && !userSocket.isClosed()) userSocket.close();
        if (hrSocket != null && !hrSocket.isClosed()) hrSocket.close();
        if (adminSocket != null && !adminSocket.isClosed()) adminSocket.close();
        super.tearDownBase();
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

    private Response adminMessage(String command, Object data) throws Exception {
        Message msg = new Message("REQUEST", command, data);
        adminOut.println(objectMapper.writeValueAsString(msg));
        String response = adminIn.readLine();
        assertNotNull(response);
        return objectMapper.readValue(response, Response.class);
    }

    @org.junit.jupiter.api.Test
    void testAnalyticsAggregation() throws Exception {
        String hrEmail = "hranalytics" + System.currentTimeMillis() + "@test.com";
        String hrPassword = "hrpass123";

        hrMessage("REGISTER", new RegisterRequest(hrEmail, hrPassword, "HR", "HRAnalytics", "TestCompany"));
        hrMessage("LOGIN", new LoginRequest(hrEmail, hrPassword));

        Response tagsResp = hrMessage("GET_ALL_TAGS", null);
        assertEquals("OK", tagsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> allTags = (List<LinkedHashMap<String, Object>>) tagsResp.getData();
        assertNotNull(allTags);
        assertTrue(allTags.size() >= 3, "Need at least 3 tags");

        int javaTagId = ((Number) allTags.get(0).get("id")).intValue();
        int pythonTagId = ((Number) allTags.get(1).get("id")).intValue();
        int sqlTagId = allTags.size() > 2 ? ((Number) allTags.get(2).get("id")).intValue() : javaTagId;

        VacancyCreateRequest vacancy1 = new VacancyCreateRequest(
            "Java Senior Developer",
            "Senior Java developer needed",
            "Минск",
            2500,
            4000,
            "FULL_TIME",
            Arrays.asList(javaTagId)
        );
        Response vac1Resp = hrMessage("CREATE_VACANCY", vacancy1);
        assertEquals("OK", vac1Resp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> vacancy1Map = (LinkedHashMap<String, Object>) vac1Resp.getData();
        int vacancy1Id = ((Number) vacancy1Map.get("id")).intValue();

        VacancyCreateRequest vacancy2 = new VacancyCreateRequest(
            "Python Developer",
            "Python developer needed",
            "Гродно",
            2000,
            3000,
            "FULL_TIME",
            Arrays.asList(pythonTagId)
        );
        Response vac2Resp = hrMessage("CREATE_VACANCY", vacancy2);
        assertEquals("OK", vac2Resp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> vacancy2Map = (LinkedHashMap<String, Object>) vac2Resp.getData();
        int vacancy2Id = ((Number) vacancy2Map.get("id")).intValue();

        VacancyCreateRequest vacancy3 = new VacancyCreateRequest(
            "Java Python Developer",
            "Full stack developer",
            "Минск",
            2500,
            3500,
            "FULL_TIME",
            Arrays.asList(javaTagId, pythonTagId)
        );
        Response vac3Resp = hrMessage("CREATE_VACANCY", vacancy3);
        assertEquals("OK", vac3Resp.getStatus());

        String userEmail = "analyticsuser" + System.currentTimeMillis() + "@test.com";
        String userPassword = "userpass123";

        userMessage("REGISTER", new RegisterRequest(userEmail, userPassword, "USER", "AnalyticsUser", "User"));
        userMessage("LOGIN", new LoginRequest(userEmail, userPassword));

        UserTagsUpdateRequest userTagsReq = new UserTagsUpdateRequest();
        userTagsReq.setTagIds(Arrays.asList(javaTagId, sqlTagId));
        Response updateTagsResp = userMessage("UPDATE_USER_TAGS", userTagsReq);
        assertEquals("OK", updateTagsResp.getStatus());

        Response salaryStatsResp = userMessage("GET_SALARY_STATS", new LinkedHashMap<String, Object>() {{
            put("periodDays", 30);
        }});
        assertEquals("OK", salaryStatsResp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> salaryStats = (LinkedHashMap<String, Object>) salaryStatsResp.getData();
        assertNotNull(salaryStats);
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> dataPoints = (List<LinkedHashMap<String, Object>>) salaryStats.get("dataPoints");
        assertNotNull(dataPoints);
        assertFalse(dataPoints.isEmpty(), "Expected salary data points for user with tags");

        Response topTagsResp = hrMessage("GET_TOP_TAGS", new LinkedHashMap<String, Object>() {{
            put("periodDays", 30);
            put("limit", 10);
        }});
        assertEquals("OK", topTagsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> topTags = (List<LinkedHashMap<String, Object>>) topTagsResp.getData();
        assertNotNull(topTags);
        assertFalse(topTags.isEmpty(), "Expected top tags");

        Response geoVacResp = hrMessage("GET_GEO_STATS", new LinkedHashMap<String, Object>() {{
            put("type", "VACANCY");
            put("periodDays", 30);
        }});
        assertEquals("OK", geoVacResp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> geoVac = (LinkedHashMap<String, Object>) geoVacResp.getData();
        assertNotNull(geoVac);

        Response geoAppResp = hrMessage("GET_GEO_STATS", new LinkedHashMap<String, Object>() {{
            put("type", "APPLICANT");
            put("periodDays", 30);
        }});
        assertEquals("OK", geoAppResp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> geoApp = (LinkedHashMap<String, Object>) geoAppResp.getData();
        assertNotNull(geoApp);

        Response conversionResp = hrMessage("GET_CONVERSION", new LinkedHashMap<String, Object>() {{
            put("vacancyId", vacancy1Id);
        }});
        assertEquals("OK", conversionResp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> conversion = (LinkedHashMap<String, Object>) conversionResp.getData();
        assertNotNull(conversion);
        Number views = (Number) conversion.get("views");
        assertNotNull(views);
        assertEquals(0, views.intValue(), "New vacancy should have 0 views initially");

        Response salaryCompResp = hrMessage("GET_SALARY_COMPARISON", new LinkedHashMap<String, Object>() {{
            put("tagIds", Arrays.asList(javaTagId, pythonTagId));
        }});
        assertEquals("OK", salaryCompResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> salaryComp = (List<LinkedHashMap<String, Object>>) salaryCompResp.getData();
        assertNotNull(salaryComp);
        assertFalse(salaryComp.isEmpty(), "Expected salary comparison data");
    }

    @org.junit.jupiter.api.Test
    void testAnalyticsForAdmin() throws Exception {
        String adminEmail = "adminanalytics" + System.currentTimeMillis() + "@test.com";
        String adminPassword = "adminpass123";

        adminMessage("REGISTER", new RegisterRequest(adminEmail, adminPassword, "ADMIN", "AdminAnalytics", "Admin"));
        adminMessage("LOGIN", new LoginRequest(adminEmail, adminPassword));

        Response sysStatsResp = adminMessage("GET_SYSTEM_STATS", null);
        assertEquals("OK", sysStatsResp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> sysStats = (LinkedHashMap<String, Object>) sysStatsResp.getData();
        assertNotNull(sysStats);
        Number totalUsers = (Number) sysStats.get("totalUsers");
        assertNotNull(totalUsers);
        assertTrue(totalUsers.intValue() >= 1, "Should have at least 1 user (admin)");

        Response activityResp = adminMessage("GET_ACTIVITY_TIMELINE", new LinkedHashMap<String, Object>() {{
            put("days", 30);
        }});
        assertEquals("OK", activityResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> activity = (List<LinkedHashMap<String, Object>>) activityResp.getData();
        assertNotNull(activity);
    }

    @org.junit.jupiter.api.Test
    void testSalaryStatsPeriodFilter() throws Exception {
        String hrEmail = "hrperiod" + System.currentTimeMillis() + "@test.com";
        String hrPassword = "hrpass123";

        hrMessage("REGISTER", new RegisterRequest(hrEmail, hrPassword, "HR", "HRPeriod", "TestCompany"));
        hrMessage("LOGIN", new LoginRequest(hrEmail, hrPassword));

        Response tagsResp = hrMessage("GET_ALL_TAGS", null);
        assertEquals("OK", tagsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> allTags = (List<LinkedHashMap<String, Object>>) tagsResp.getData();
        int tagId = ((Number) allTags.get(0).get("id")).intValue();

        VacancyCreateRequest vacancy = new VacancyCreateRequest(
            "Test Vacancy",
            "Description",
            "Минск",
            2000,
            3000,
            "FULL_TIME",
            Arrays.asList(tagId)
        );
        hrMessage("CREATE_VACANCY", vacancy);

        String userEmail = "userperiod" + System.currentTimeMillis() + "@test.com";
        String userPassword = "userpass123";

        userMessage("REGISTER", new RegisterRequest(userEmail, userPassword, "USER", "UserPeriod", "User"));
        userMessage("LOGIN", new LoginRequest(userEmail, userPassword));

        UserTagsUpdateRequest userTagsReq = new UserTagsUpdateRequest();
        userTagsReq.setTagIds(Arrays.asList(tagId));
        Response updateTagsResp = userMessage("UPDATE_USER_TAGS", userTagsReq);
        assertEquals("OK", updateTagsResp.getStatus());

        Response statsWeekResp = userMessage("GET_SALARY_STATS", new LinkedHashMap<String, Object>() {{
            put("periodDays", 7);
        }});
        assertEquals("OK", statsWeekResp.getStatus());

        Response statsMonthResp = userMessage("GET_SALARY_STATS", new LinkedHashMap<String, Object>() {{
            put("periodDays", 30);
        }});
        assertEquals("OK", statsMonthResp.getStatus());

        Response statsYearResp = userMessage("GET_SALARY_STATS", new LinkedHashMap<String, Object>() {{
            put("periodDays", 365);
        }});
        assertEquals("OK", statsYearResp.getStatus());
    }
}