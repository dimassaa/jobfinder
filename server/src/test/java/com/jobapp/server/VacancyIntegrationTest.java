package com.jobapp.server;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.*;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VacancyIntegrationTest extends BaseIntegrationTest {

    @Test
    void testVacancyFlow() throws Exception {
        String testEmail = "hr" + System.currentTimeMillis() + "@test.com";
        String testPassword = "testPassword123";

        Message registerMsg = new Message("REQUEST", "REGISTER",
            new RegisterRequest(testEmail, testPassword, "HR", "TestHR", "Company"));
        out.println(objectMapper.writeValueAsString(registerMsg));

        String registerResponse = in.readLine();
        assertNotNull(registerResponse);
        Response registerResp = objectMapper.readValue(registerResponse, Response.class);
        assertEquals("OK", registerResp.getStatus());

        Message loginMsg = new Message("REQUEST", "LOGIN", new LoginRequest(testEmail, testPassword));
        out.println(objectMapper.writeValueAsString(loginMsg));

        String loginResponse = in.readLine();
        assertNotNull(loginResponse);
        Response loginResp = objectMapper.readValue(loginResponse, Response.class);
        assertEquals("OK", loginResp.getStatus());

        Message getAllTagsMsg = new Message("REQUEST", "GET_ALL_TAGS", null);
        out.println(objectMapper.writeValueAsString(getAllTagsMsg));

        String getAllTagsResponse = in.readLine();
        assertNotNull(getAllTagsResponse);
        Response getAllTagsResp = objectMapper.readValue(getAllTagsResponse, Response.class);
        assertEquals("OK", getAllTagsResp.getStatus());

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> allTagsMap = (List<LinkedHashMap<String, Object>>) getAllTagsResp.getData();
        assertNotNull(allTagsMap);
        assertTrue(allTagsMap.size() >= 1);

        int tag1Id = ((Number) allTagsMap.get(0).get("id")).intValue();
        int tag2Id = allTagsMap.size() > 1 ? ((Number) allTagsMap.get(1).get("id")).intValue() : tag1Id;

        VacancyCreateRequest createReq = new VacancyCreateRequest(
            "Java Developer",
            "Looking for an experienced Java developer",
            "Минск",
            2000,
            3000,
            "FULL_TIME",
            Arrays.asList(tag1Id, tag2Id)
        );

        Message createVacancyMsg = new Message("REQUEST", "CREATE_VACANCY", createReq);
        out.println(objectMapper.writeValueAsString(createVacancyMsg));

        String createVacancyResponse = in.readLine();
        assertNotNull(createVacancyResponse);
        Response createVacancyResp = objectMapper.readValue(createVacancyResponse, Response.class);
        assertEquals("OK", createVacancyResp.getStatus());

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> createdVacancy = (LinkedHashMap<String, Object>) createVacancyResp.getData();
        assertNotNull(createdVacancy);
        int vacancyId = ((Number) createdVacancy.get("id")).intValue();
        assertEquals("Java Developer", createdVacancy.get("title"));
        assertEquals("Минск", createdVacancy.get("city"));

        Message getMyVacanciesMsg = new Message("REQUEST", "GET_MY_VACANCIES", null);
        out.println(objectMapper.writeValueAsString(getMyVacanciesMsg));

        String getMyVacanciesResponse = in.readLine();
        assertNotNull(getMyVacanciesResponse);
        Response getMyVacanciesResp = objectMapper.readValue(getMyVacanciesResponse, Response.class);
        assertEquals("OK", getMyVacanciesResp.getStatus());

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> myVacancies = (List<LinkedHashMap<String, Object>>) getMyVacanciesResp.getData();
        assertNotNull(myVacancies);
        assertTrue(myVacancies.size() >= 1);
        boolean found = false;
        for (LinkedHashMap<String, Object> v : myVacancies) {
            if (((Number) v.get("id")).intValue() == vacancyId) {
                found = true;
                assertEquals("Java Developer", v.get("title"));
                break;
            }
        }
        assertTrue(found, "Created vacancy should be in my vacancies list");

        VacancyFilterDto searchFilter = new VacancyFilterDto();
        searchFilter.setCity("Минск");
        searchFilter.setKeyword("Java");
        Message searchMsg = new Message("REQUEST", "SEARCH_VACANCIES", searchFilter);
        out.println(objectMapper.writeValueAsString(searchMsg));

        String searchResponse = in.readLine();
        assertNotNull(searchResponse);
        Response searchResp = objectMapper.readValue(searchResponse, Response.class);
        assertEquals("OK", searchResp.getStatus());

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> searchResults = (List<LinkedHashMap<String, Object>>) searchResp.getData();
        assertNotNull(searchResults);
        assertTrue(searchResults.size() >= 1);
        boolean foundInSearch = false;
        for (LinkedHashMap<String, Object> v : searchResults) {
            if (((Number) v.get("id")).intValue() == vacancyId) {
                foundInSearch = true;
                assertEquals("Java Developer", v.get("title"));
                break;
            }
        }
        assertTrue(foundInSearch, "Vacancy should be found in search by keyword");

        VacancyFilterDto cityFilter = new VacancyFilterDto();
        cityFilter.setCity("Минск");
        Message citySearchMsg = new Message("REQUEST", "SEARCH_VACANCIES", cityFilter);
        out.println(objectMapper.writeValueAsString(citySearchMsg));

        String citySearchResponse = in.readLine();
        assertNotNull(citySearchResponse);
        Response citySearchResp = objectMapper.readValue(citySearchResponse, Response.class);
        assertEquals("OK", citySearchResp.getStatus());

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> cityResults = (List<LinkedHashMap<String, Object>>) citySearchResp.getData();
        assertNotNull(cityResults);
        assertTrue(cityResults.size() >= 1);

        VacancyCreateRequest updateReq = new VacancyCreateRequest(
            "Senior Java Developer",
            "Looking for an experienced Java developer",
            "Минск",
            2500,
            3500,
            "FULL_TIME",
            Arrays.asList(tag1Id, tag2Id)
        );

        VacancyUpdateRequest updateRequest = new VacancyUpdateRequest();
        updateRequest.setVacancyId(vacancyId);
        updateRequest.setVacancy(updateReq);
        Message updateMsg = new Message("REQUEST", "UPDATE_VACANCY", updateRequest);
        out.println(objectMapper.writeValueAsString(updateMsg));

        String updateResponse = in.readLine();
        assertNotNull(updateResponse);
        Response updateResp = objectMapper.readValue(updateResponse, Response.class);
        assertEquals("OK", updateResp.getStatus());

        Message getDetailsMsg = new Message("REQUEST", "GET_VACANCY_DETAILS", vacancyId);
        out.println(objectMapper.writeValueAsString(getDetailsMsg));

        String getDetailsResponse = in.readLine();
        assertNotNull(getDetailsResponse);
        Response getDetailsResp = objectMapper.readValue(getDetailsResponse, Response.class);
        assertEquals("OK", getDetailsResp.getStatus());

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> updatedVacancy = (LinkedHashMap<String, Object>) getDetailsResp.getData();
        assertEquals("Senior Java Developer", updatedVacancy.get("title"));
        assertEquals(2500, ((Number) updatedVacancy.get("salaryMin")).intValue());

        Message closeMsg = new Message("REQUEST", "CLOSE_VACANCY", vacancyId);
        out.println(objectMapper.writeValueAsString(closeMsg));

        String closeResponse = in.readLine();
        assertNotNull(closeResponse);
        Response closeResp = objectMapper.readValue(closeResponse, Response.class);
        assertEquals("OK", closeResp.getStatus());

        VacancyFilterDto searchAfterClose = new VacancyFilterDto();
        searchAfterClose.setCity("Минск");
        searchAfterClose.setKeyword("Java");
        Message searchAfterCloseMsg = new Message("REQUEST", "SEARCH_VACANCIES", searchAfterClose);
        out.println(objectMapper.writeValueAsString(searchAfterCloseMsg));

        String searchAfterCloseResponse = in.readLine();
        assertNotNull(searchAfterCloseResponse);
        Response searchAfterCloseResp = objectMapper.readValue(searchAfterCloseResponse, Response.class);
        assertEquals("OK", searchAfterCloseResp.getStatus());

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> searchAfterCloseResults = (List<LinkedHashMap<String, Object>>) searchAfterCloseResp.getData();
        assertNotNull(searchAfterCloseResults);
        for (LinkedHashMap<String, Object> v : searchAfterCloseResults) {
            assertNotEquals(vacancyId, ((Number) v.get("id")).intValue());
        }
    }

    @Test
    void testGuestSearchVacancies() throws Exception {
        VacancyFilterDto filter = new VacancyFilterDto();
        Message searchMsg = new Message("REQUEST", "SEARCH_VACANCIES", filter);
        out.println(objectMapper.writeValueAsString(searchMsg));

        String searchResponse = in.readLine();
        assertNotNull(searchResponse);
        Response searchResp = objectMapper.readValue(searchResponse, Response.class);
        assertEquals("OK", searchResp.getStatus());

        Message closeVacancyMsg = new Message("REQUEST", "CLOSE_VACANCY", 999);
        out.println(objectMapper.writeValueAsString(closeVacancyMsg));

        String closeResponse = in.readLine();
        assertNotNull(closeResponse);
        Response closeResp = objectMapper.readValue(closeResponse, Response.class);
        assertEquals("ERROR", closeResp.getStatus());
    }

    @Test
    void testNonHRCannotCreateVacancy() throws Exception {
        String testEmail = "user" + System.currentTimeMillis() + "@test.com";
        String testPassword = "testPassword123";

        Message registerMsg = new Message("REQUEST", "REGISTER",
            new RegisterRequest(testEmail, testPassword, "USER", "Test", "User"));
        out.println(objectMapper.writeValueAsString(registerMsg));

        String registerResponse = in.readLine();
        assertNotNull(registerResponse);
        Response registerResp = objectMapper.readValue(registerResponse, Response.class);
        assertEquals("OK", registerResp.getStatus());

        Message loginMsg = new Message("REQUEST", "LOGIN", new LoginRequest(testEmail, testPassword));
        out.println(objectMapper.writeValueAsString(loginMsg));

        String loginResponse = in.readLine();
        assertNotNull(loginResponse);
        Response loginResp = objectMapper.readValue(loginResponse, Response.class);
        assertEquals("OK", loginResp.getStatus());

        VacancyCreateRequest createReq = new VacancyCreateRequest(
            "Test Vacancy",
            "Description",
            "Минск",
            1500,
            2500,
            "FULL_TIME",
            null
        );

        Message createVacancyMsg = new Message("REQUEST", "CREATE_VACANCY", createReq);
        out.println(objectMapper.writeValueAsString(createVacancyMsg));

        String createVacancyResponse = in.readLine();
        assertNotNull(createVacancyResponse);
        Response createVacancyResp = objectMapper.readValue(createVacancyResponse, Response.class);
        assertEquals("ERROR", createVacancyResp.getStatus());
    }

    @Test
    void testSearchBySalaryRange() throws Exception {
        String hrEmail = "hrsalary" + System.currentTimeMillis() + "@test.com";
        message("REGISTER", new RegisterRequest(hrEmail, "pass123", "HR", "Test", "HR"));
        message("LOGIN", new LoginRequest(hrEmail, "pass123"));

        message("GET_ALL_TAGS", null);

        VacancyCreateRequest highSalaryReq = new VacancyCreateRequest(
            "High Salary Dev",
            "High salary position",
            "Гродно",
            3000,
            5000,
            "FULL_TIME",
            null
        );
        message("CREATE_VACANCY", highSalaryReq);

        VacancyFilterDto salaryFilter = new VacancyFilterDto();
        salaryFilter.setSalaryFrom(2500);
        salaryFilter.setSalaryTo(5000);
        Response searchResp = message("SEARCH_VACANCIES", salaryFilter);
        assertEquals("OK", searchResp.getStatus());

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> results = (List<LinkedHashMap<String, Object>>) searchResp.getData();
        assertNotNull(results);
    }

    @Test
    void testSearchByEmploymentType() throws Exception {
        String hrEmail = "hremptype" + System.currentTimeMillis() + "@test.com";
        message("REGISTER", new RegisterRequest(hrEmail, "pass123", "HR", "Test", "HR"));
        message("LOGIN", new LoginRequest(hrEmail, "pass123"));

        message("GET_ALL_TAGS", null);

        VacancyCreateRequest partTimeReq = new VacancyCreateRequest(
            "Part Time Dev",
            "Part time position",
            "Минск",
            500,
            1000,
            "PART_TIME",
            null
        );
        message("CREATE_VACANCY", partTimeReq);

        VacancyFilterDto typeFilter = new VacancyFilterDto();
        typeFilter.setEmploymentType("PART_TIME");
        Response searchResp = message("SEARCH_VACANCIES", typeFilter);
        assertEquals("OK", searchResp.getStatus());

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> results = (List<LinkedHashMap<String, Object>>) searchResp.getData();
        assertNotNull(results);
    }
}