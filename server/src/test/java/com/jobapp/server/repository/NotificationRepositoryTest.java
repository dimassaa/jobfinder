package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRepositoryTest {

    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() throws Exception {
        notificationRepository = new NotificationRepository();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM notifications")) {
            stmt.executeUpdate();
        }
    }

    @Test
    void testFindAllByUserId_WithPagination() throws Exception {
        for (int i = 1; i <= 15; i++) {
            Notification n = new Notification();
            n.setUserId(1);
            n.setType("TEST");
            n.setTitle("Title " + i);
            n.setContent("Content " + i);
            n.setLink("/test/" + i);
            n.setIsRead(false);
            notificationRepository.save(n);
        }

        List<Notification> result = notificationRepository.findAllByUserId(1, 10, 0);

        assertNotNull(result);
        assertEquals(10, result.size());
    }

    @Test
    void testFindAllByUserId_WithOffset() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Notification n = new Notification();
            n.setUserId(1);
            n.setType("TEST");
            n.setTitle("Title " + i);
            n.setContent("Content " + i);
            n.setLink("/test/" + i);
            n.setIsRead(false);
            notificationRepository.save(n);
        }

        List<Notification> result = notificationRepository.findAllByUserId(1, 10, 3);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFindAllByUserId_EmptyResult() {
        List<Notification> result = notificationRepository.findAllByUserId(999, 10, 0);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testMarkAsRead() throws Exception {
        Notification n = new Notification();
        n.setUserId(1);
        n.setType("TEST");
        n.setTitle("Test");
        n.setContent("Test");
        n.setIsRead(false);
        notificationRepository.save(n);

        notificationRepository.markAsRead(n.getId());

        Notification result = notificationRepository.findById(n.getId()).orElse(null);
        assertNotNull(result);
        assertTrue(result.getIsRead());
    }

    @Test
    void testMarkAllAsReadByUserId() throws Exception {
        for (int i = 1; i <= 3; i++) {
            Notification n = new Notification();
            n.setUserId(1);
            n.setType("TEST");
            n.setTitle("Title " + i);
            n.setContent("Content " + i);
            n.setIsRead(false);
            notificationRepository.save(n);
        }

        notificationRepository.markAllAsReadByUserId(1);

        List<Notification> unread = notificationRepository.findUnreadByUserId(1);
        assertTrue(unread.isEmpty());
    }

    @Test
    void testDelete() throws Exception {
        Notification n = new Notification();
        n.setUserId(1);
        n.setType("TEST");
        n.setTitle("Test");
        n.setContent("Test");
        n.setIsRead(false);
        notificationRepository.save(n);
        int id = n.getId();

        notificationRepository.delete(id);

        assertTrue(notificationRepository.findById(id).isEmpty());
    }

    @Test
    void testCountUnreadByUserId() throws Exception {
        for (int i = 1; i <= 3; i++) {
            Notification n = new Notification();
            n.setUserId(1);
            n.setType("TEST");
            n.setTitle("Title " + i);
            n.setContent("Content " + i);
            n.setIsRead(false);
            notificationRepository.save(n);
        }
        Notification read = new Notification();
        read.setUserId(1);
        read.setType("TEST");
        read.setTitle("Read");
        read.setContent("Read");
        read.setIsRead(true);
        notificationRepository.save(read);

        int count = notificationRepository.countUnreadByUserId(1);

        assertEquals(3, count);
    }
}