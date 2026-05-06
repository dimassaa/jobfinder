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

class ApplicationIntegrationTest extends BaseIntegrationTest {

    private Socket userSocket;
    private PrintWriter userOut;
    private BufferedReader userIn;

    @AfterEach
    void cleanup() throws Exception {
        if (userSocket != null && !userSocket.isClosed()) userSocket.close();
    }

    private void connectUser() throws Exception {
        userSocket = new Socket("localhost", testPort);
        userOut = new PrintWriter(userSocket.getOutputStream(), true);
        userIn = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
        Thread.sleep(300);
    }

    private Response userMessage(String command, Object data) throws Exception {
        Message msg = new Message("REQUEST", command, data);
        userOut.println(objectMapper.writeValueAsString(msg));
        String response = userIn.readLine();
        assertNotNull(response);
        return objectMapper.readValue(response, Response.class);
    }

    @Test
    void testFullApplicationFlow() throws Exception {
        String hrEmail = "hrflow" + System.currentTimeMillis() + "@test.com";
        String hrPassword = "hrpass123";
        String userEmail = "userflow" + System.currentTimeMillis() + "@test.com";
        String userPassword = "userpass123";

        Response regResp = message("REGISTER", new RegisterRequest(hrEmail, hrPassword, "HR", "TestHR", "Company"));
        assertEquals("OK", regResp.getStatus());

        connectUser();
        userMessage("REGISTER", new RegisterRequest(userEmail, userPassword, "USER", "Test", "User"));

        message("LOGIN", new LoginRequest(hrEmail, hrPassword));

        Response tagsResp = message("GET_ALL_TAGS", null);
        assertEquals("OK", tagsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> allTags = (List<LinkedHashMap<String, Object>>) tagsResp.getData();
        assertNotNull(allTags);
        assertTrue(allTags.size() >= 1);
        int tag1Id = ((Number) allTags.get(0).get("id")).intValue();
        int tag2Id = allTags.size() > 1 ? ((Number) allTags.get(1).get("id")).intValue() : tag1Id;

        VacancyCreateRequest vacancyReq = new VacancyCreateRequest(
            "Java Developer",
            "Looking for Java developer",
            "Minsk",
            2000,
            3000,
            "FULL_TIME",
            Arrays.asList(tag1Id, tag2Id)
        );
        Response createResp = message("CREATE_VACANCY", vacancyReq);
        assertEquals("OK", createResp.getStatus());

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> createdVacancy = (LinkedHashMap<String, Object>) createResp.getData();
        int vacancyId = ((Number) createdVacancy.get("id")).intValue();

        userMessage("LOGIN", new LoginRequest(userEmail, userPassword));

        FavoriteRequest addViewReq = new FavoriteRequest(vacancyId);
        Response addViewResp = userMessage("ADD_VIEW", addViewReq);
        assertEquals("OK", addViewResp.getStatus());

        FavoriteRequest addFavReq = new FavoriteRequest(vacancyId);
        Response addFavResp = userMessage("ADD_FAVORITE", addFavReq);
        assertEquals("OK", addFavResp.getStatus());

        FavoriteRequest applyReq = new FavoriteRequest(vacancyId);
        Response applyResp = userMessage("APPLY", applyReq);
        assertEquals("OK", applyResp.getStatus());

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> application = (LinkedHashMap<String, Object>) applyResp.getData();
        int applicationId = ((Number) application.get("id")).intValue();
        assertEquals("PENDING", application.get("status"));

        Response myAppsResp = userMessage("GET_MY_APPLICATIONS", null);
        assertEquals("OK", myAppsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> myApps = (List<LinkedHashMap<String, Object>>) myAppsResp.getData();
        assertNotNull(myApps);
        assertTrue(myApps.size() >= 1);

        boolean foundMyApp = false;
        for (LinkedHashMap<String, Object> app : myApps) {
            if (((Number) app.get("id")).intValue() == applicationId) {
                foundMyApp = true;
                assertEquals("PENDING", app.get("status"));
                break;
            }
        }
        assertTrue(foundMyApp, "Application should be in user's applications");

        FavoriteRequest hrGetAppsReq = new FavoriteRequest(vacancyId);
        Response hrAppsResp = message("GET_APPLICATIONS_FOR_VACANCY", hrGetAppsReq);
        assertEquals("OK", hrAppsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> hrApps = (List<LinkedHashMap<String, Object>>) hrAppsResp.getData();
        assertNotNull(hrApps);
        assertTrue(hrApps.size() >= 1);

        boolean foundHrApp = false;
        for (LinkedHashMap<String, Object> app : hrApps) {
            if (((Number) app.get("id")).intValue() == applicationId) {
                foundHrApp = true;
                assertEquals("PENDING", app.get("status"));
                break;
            }
        }
        assertTrue(foundHrApp, "HR should see the application");

        ApplicationStatusUpdateRequest statusUpdate = new ApplicationStatusUpdateRequest();
        statusUpdate.setApplicationId(applicationId);
        statusUpdate.setNewStatus("INVITED");
        Response updateStatusResp = message("UPDATE_APPLICATION_STATUS", statusUpdate);
        assertEquals("OK", updateStatusResp.getStatus());

        Response updatedAppsResp = userMessage("GET_MY_APPLICATIONS", null);
        assertEquals("OK", updatedAppsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> updatedApps = (List<LinkedHashMap<String, Object>>) updatedAppsResp.getData();
        assertNotNull(updatedApps);

        boolean foundUpdated = false;
        for (LinkedHashMap<String, Object> app : updatedApps) {
            if (((Number) app.get("id")).intValue() == applicationId) {
                foundUpdated = true;
                assertEquals("INVITED", app.get("status"));
                break;
            }
        }
        assertTrue(foundUpdated, "Application status should be updated to INVITED");

        ComplaintCreateRequest complaintReq = new ComplaintCreateRequest();
        complaintReq.setTargetType("VACANCY");
        complaintReq.setTargetId(vacancyId);
        complaintReq.setReason("Spam vacancy");
        Response complaintResp = userMessage("CREATE_COMPLAINT", complaintReq);
        assertEquals("OK", complaintResp.getStatus());
    }

    @Test
    void testFavoritesFlow() throws Exception {
        String favUserEmail = "favuser" + System.currentTimeMillis() + "@test.com";
        String favUserPassword = "favpass123";

        message("REGISTER", new RegisterRequest(favUserEmail, favUserPassword, "USER", "FavUser", "User"));

        connectUser();
        userMessage("LOGIN", new LoginRequest(favUserEmail, favUserPassword));

        Response tagsResp = userMessage("GET_ALL_TAGS", null);
        assertEquals("OK", tagsResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> allTags = (List<LinkedHashMap<String, Object>>) tagsResp.getData();
        int tagId = ((Number) allTags.get(0).get("id")).intValue();

        String hrFavEmail = "hrfav" + System.currentTimeMillis() + "@test.com";
        String hrFavPassword = "hrfavpass123";
        message("REGISTER", new RegisterRequest(hrFavEmail, hrFavPassword, "HR", "HR", "Company"));
        message("LOGIN", new LoginRequest(hrFavEmail, hrFavPassword));

        VacancyCreateRequest vacancyReq = new VacancyCreateRequest(
            "Test Vacancy",
            "Description",
            "Minsk",
            1500,
            2500,
            "FULL_TIME",
            Arrays.asList(tagId)
        );
        Response createResp = message("CREATE_VACANCY", vacancyReq);
        assertEquals("OK", createResp.getStatus());
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> vacancy = (LinkedHashMap<String, Object>) createResp.getData();
        int vacancyId = ((Number) vacancy.get("id")).intValue();

        FavoriteRequest addFavReq = new FavoriteRequest(vacancyId);
        Response addFavResp = userMessage("ADD_FAVORITE", addFavReq);
        assertEquals("OK", addFavResp.getStatus());

        Response getFavResp = userMessage("GET_FAVORITES", null);
        assertEquals("OK", getFavResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> favorites = (List<LinkedHashMap<String, Object>>) getFavResp.getData();
        assertNotNull(favorites);
        assertTrue(favorites.size() >= 1);

        boolean foundFav = false;
        for (LinkedHashMap<String, Object> f : favorites) {
            if (((Number) f.get("id")).intValue() == vacancyId) {
                foundFav = true;
                break;
            }
        }
        assertTrue(foundFav, "Vacancy should be in favorites");

        FavoriteRequest removeFavReq = new FavoriteRequest(vacancyId);
        Response removeFavResp = userMessage("REMOVE_FAVORITE", removeFavReq);
        assertEquals("OK", removeFavResp.getStatus());

        Response getFavAfterRemoveResp = userMessage("GET_FAVORITES", null);
        assertEquals("OK", getFavAfterRemoveResp.getStatus());
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> favoritesAfterRemove = (List<LinkedHashMap<String, Object>>) getFavAfterRemoveResp.getData();
        assertNotNull(favoritesAfterRemove);

        boolean foundFavAfterRemove = false;
        for (LinkedHashMap<String, Object> f : favoritesAfterRemove) {
            if (((Number) f.get("id")).intValue() == vacancyId) {
                foundFavAfterRemove = true;
                break;
            }
        }
        assertFalse(foundFavAfterRemove, "Vacancy should be removed from favorites");
    }

    @Test
    void testDuplicateApplicationRejected() throws Exception {
        String dupUserEmail = "dupuser" + System.currentTimeMillis() + "@test.com";
        String dupUserPassword = "duppass123";

        connectUser();
        userMessage("REGISTER", new RegisterRequest(dupUserEmail, dupUserPassword, "USER", "DupUser", "User"));
        userMessage("LOGIN", new LoginRequest(dupUserEmail, dupUserPassword));

        Response tagsResp = userMessage("GET_ALL_TAGS", null);
        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> allTags = (List<LinkedHashMap<String, Object>>) tagsResp.getData();
        int tagId = ((Number) allTags.get(0).get("id")).intValue();

        String hrDupEmail = "hrdup" + System.currentTimeMillis() + "@test.com";
        String hrDupPassword = "hrdupass123";
        message("REGISTER", new RegisterRequest(hrDupEmail, hrDupPassword, "HR", "HR", "Company"));
        message("LOGIN", new LoginRequest(hrDupEmail, hrDupPassword));

        VacancyCreateRequest vacancyReq = new VacancyCreateRequest(
            "Dup Test Vacancy",
            "Description",
            "Minsk",
            2000,
            3000,
            "FULL_TIME",
            Arrays.asList(tagId)
        );
        Response createResp = message("CREATE_VACANCY", vacancyReq);
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> vacancy = (LinkedHashMap<String, Object>) createResp.getData();
        int vacancyId = ((Number) vacancy.get("id")).intValue();

        FavoriteRequest applyReq1 = new FavoriteRequest(vacancyId);
        Response applyResp1 = userMessage("APPLY", applyReq1);
        assertEquals("OK", applyResp1.getStatus());

        FavoriteRequest applyReq2 = new FavoriteRequest(vacancyId);
        Response applyResp2 = userMessage("APPLY", applyReq2);
        assertEquals("ERROR", applyResp2.getStatus());
    }
}