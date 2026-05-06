package com.jobapp.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AdminDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Test
    void testAdminUserDtoSerialization() throws Exception {
        AdminUserDto dto = new AdminUserDto(1, "admin@test.com", "ADMIN", "John", "Doe", "Minsk", false, null, LocalDateTime.now());
        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"email\":\"admin@test.com\""));
        assertTrue(json.contains("\"role\":\"ADMIN\""));

        AdminUserDto deserialized = objectMapper.readValue(json, AdminUserDto.class);
        assertEquals("admin@test.com", deserialized.getEmail());
        assertEquals("ADMIN", deserialized.getRole());
    }

    @Test
    void testBlockUserRequestSerialization() throws Exception {
        BlockUserRequest req = new BlockUserRequest(1, "Spam", 24);
        String json = objectMapper.writeValueAsString(req);
        assertTrue(json.contains("\"userId\":1"));
        assertTrue(json.contains("\"reason\":\"Spam\""));

        BlockUserRequest deserialized = objectMapper.readValue(json, BlockUserRequest.class);
        assertEquals(1, deserialized.getUserId());
        assertEquals("Spam", deserialized.getReason());
        assertEquals(24, deserialized.getDurationHours());
    }

    @Test
    void testBlockUserRequestNullDuration() throws Exception {
        BlockUserRequest req = new BlockUserRequest(1, "Permanent", null);
        String json = objectMapper.writeValueAsString(req);
        assertTrue(json.contains("\"durationHours\":null") || !json.contains("durationHours"));

        BlockUserRequest deserialized = objectMapper.readValue(json, BlockUserRequest.class);
        assertNull(deserialized.getDurationHours());
    }

    @Test
    void testComplaintAdminDtoSerialization() throws Exception {
        ComplaintAdminDto dto = new ComplaintAdminDto(1, 1, "VACANCY", 100, "Bad", "PENDING", LocalDateTime.now(), null, null, "Vacancy Title");
        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"targetName\":\"Vacancy Title\""));

        ComplaintAdminDto deserialized = objectMapper.readValue(json, ComplaintAdminDto.class);
        assertEquals("Vacancy Title", deserialized.getTargetName());
        assertEquals("VACANCY", deserialized.getTargetType());
    }

    @Test
    void testResolveComplaintRequestSerialization() throws Exception {
        ResolveComplaintRequest req = new ResolveComplaintRequest(1, "BLOCK_USER");
        String json = objectMapper.writeValueAsString(req);
        assertTrue(json.contains("\"action\":\"BLOCK_USER\""));

        ResolveComplaintRequest deserialized = objectMapper.readValue(json, ResolveComplaintRequest.class);
        assertEquals(1, deserialized.getComplaintId());
        assertEquals("BLOCK_USER", deserialized.getAction());
    }

    @Test
    void testAddTagRequestSerialization() throws Exception {
        AddTagRequest req = new AddTagRequest("Java");
        String json = objectMapper.writeValueAsString(req);
        assertTrue(json.contains("\"tagName\":\"Java\""));

        AddTagRequest deserialized = objectMapper.readValue(json, AddTagRequest.class);
        assertEquals("Java", deserialized.getTagName());
    }
}
