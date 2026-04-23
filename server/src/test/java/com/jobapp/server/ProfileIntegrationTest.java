package com.jobapp.server;

import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.*;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileIntegrationTest extends BaseIntegrationTest {

    @Test
    void testProfileFlow() throws Exception {
        String testEmail = "profiletest" + System.currentTimeMillis() + "@test.com";
        String testPassword = "testPassword123";

        int userId;

        Message registerMsg = new Message("REQUEST", "REGISTER",
            new RegisterRequest(testEmail, testPassword, "USER", "Test", "User"));
        out.println(objectMapper.writeValueAsString(registerMsg));

        String registerResponse = in.readLine();
        assertNotNull(registerResponse);
        Response registerResp = objectMapper.readValue(registerResponse, Response.class);
        assertEquals("OK", registerResp.getStatus());

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> registerData = (LinkedHashMap<String, Object>) registerResp.getData();
        userId = ((Number) registerData.get("userId")).intValue();

        Message loginMsg = new Message("REQUEST", "LOGIN", new LoginRequest(testEmail, testPassword));
        out.println(objectMapper.writeValueAsString(loginMsg));

        String loginResponse = in.readLine();
        assertNotNull(loginResponse);
        Response loginResp = objectMapper.readValue(loginResponse, Response.class);
        assertEquals("OK", loginResp.getStatus());

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> loginData = (LinkedHashMap<String, Object>) loginResp.getData();
        assertTrue((Boolean) loginData.get("success"));

        Message getProfileMsg = new Message("REQUEST", "GET_PROFILE", null);
        out.println(objectMapper.writeValueAsString(getProfileMsg));

        String getProfileResponse = in.readLine();
        assertNotNull(getProfileResponse);
        Response getProfileResp = objectMapper.readValue(getProfileResponse, Response.class);
        assertEquals("OK", getProfileResp.getStatus());

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> profileData = (LinkedHashMap<String, Object>) getProfileResp.getData();
        assertEquals("Test", profileData.get("name"));
        assertEquals("User", profileData.get("surname"));

        Message updateProfileMsg = new Message("REQUEST", "UPDATE_PROFILE",
            new ProfileDto("UpdatedName", "UpdatedSurname", "Moscow", "+79001234567", "Additional info"));
        out.println(objectMapper.writeValueAsString(updateProfileMsg));

        String updateProfileResponse = in.readLine();
        assertNotNull(updateProfileResponse);
        Response updateProfileResp = objectMapper.readValue(updateProfileResponse, Response.class);
        assertEquals("OK", updateProfileResp.getStatus());

        Message getProfileMsg2 = new Message("REQUEST", "GET_PROFILE", null);
        out.println(objectMapper.writeValueAsString(getProfileMsg2));

        String getProfileResponse2 = in.readLine();
        assertNotNull(getProfileResponse2);
        Response getProfileResp2 = objectMapper.readValue(getProfileResponse2, Response.class);
        assertEquals("OK", getProfileResp2.getStatus());

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> profileData2 = (LinkedHashMap<String, Object>) getProfileResp2.getData();
        assertEquals("UpdatedName", profileData2.get("name"));
        assertEquals("UpdatedSurname", profileData2.get("surname"));
        assertEquals("Moscow", profileData2.get("city"));
        assertEquals("+79001234567", profileData2.get("phone"));
        assertEquals("Additional info", profileData2.get("additionalInfo"));

        Message getAllTagsMsg = new Message("REQUEST", "GET_ALL_TAGS", null);
        out.println(objectMapper.writeValueAsString(getAllTagsMsg));

        String getAllTagsResponse = in.readLine();
        assertNotNull(getAllTagsResponse);
        Response getAllTagsResp = objectMapper.readValue(getAllTagsResponse, Response.class);
        assertEquals("OK", getAllTagsResp.getStatus());

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> allTagsMap = (List<LinkedHashMap<String, Object>>) getAllTagsResp.getData();
        assertNotNull(allTagsMap);

        int tag1Id = ((Number) allTagsMap.get(0).get("id")).intValue();
        int tag2Id = allTagsMap.size() > 1 ? ((Number) allTagsMap.get(1).get("id")).intValue() : tag1Id;
        int tag3Id = allTagsMap.size() > 2 ? ((Number) allTagsMap.get(2).get("id")).intValue() : tag1Id;

        Message updateTagsMsg = new Message("REQUEST", "UPDATE_USER_TAGS",
            new UserTagsUpdateRequest(Arrays.asList(tag1Id, tag2Id, tag3Id)));
        out.println(objectMapper.writeValueAsString(updateTagsMsg));

        String updateTagsResponse = in.readLine();
        assertNotNull(updateTagsResponse);
        Response updateTagsResp = objectMapper.readValue(updateTagsResponse, Response.class);
        assertEquals("OK", updateTagsResp.getStatus());

        Message getUserTagsMsg = new Message("REQUEST", "GET_USER_TAGS", null);
        out.println(objectMapper.writeValueAsString(getUserTagsMsg));

        String getUserTagsResponse = in.readLine();
        assertNotNull(getUserTagsResponse);
        Response getUserTagsResp = objectMapper.readValue(getUserTagsResponse, Response.class);
        assertEquals("OK", getUserTagsResp.getStatus());

        @SuppressWarnings("unchecked")
        List<LinkedHashMap<String, Object>> userTagsMap = (List<LinkedHashMap<String, Object>>) getUserTagsResp.getData();
        assertNotNull(userTagsMap);
        assertTrue(userTagsMap.size() >= 1);
    }

    @Test
    void testGetProfileWithoutLogin() throws Exception {
        Message getProfileMsg = new Message("REQUEST", "GET_PROFILE", null);
        out.println(objectMapper.writeValueAsString(getProfileMsg));

        String getProfileResponse = in.readLine();
        assertNotNull(getProfileResponse);
        Response getProfileResp = objectMapper.readValue(getProfileResponse, Response.class);
        assertEquals("ERROR", getProfileResp.getStatus());
    }
}