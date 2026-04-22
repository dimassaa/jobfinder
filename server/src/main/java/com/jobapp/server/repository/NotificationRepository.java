package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationRepository {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRepository.class);

    public Notification save(Notification notification) {
        String sql = "INSERT INTO notifications (user_id, type, title, content, link, is_read, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notification.getUserId());
            stmt.setString(2, notification.getType());
            stmt.setString(3, notification.getTitle());
            stmt.setString(4, notification.getContent());
            stmt.setString(5, notification.getLink());
            stmt.setBoolean(6, notification.getIsRead() != null ? notification.getIsRead() : false);
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    notification.setId(rs.getInt("id"));
                }
            }
            logger.info("Notification saved with id: {}", notification.getId());
            return notification;
        } catch (SQLException e) {
            logger.error("Error saving notification for user: {}", notification.getUserId(), e);
            throw new RuntimeException("Failed to save notification", e);
        }
    }

    public List<Notification> findByUserId(Integer userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching notifications by user id: {}", userId, e);
        }
        return notifications;
    }

    public List<Notification> findUnreadByUserId(Integer userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = false ORDER BY created_at DESC";
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching unread notifications by user id: {}", userId, e);
        }
        return notifications;
    }

    public void markAsRead(Integer id) {
        String sql = "UPDATE notifications SET is_read = true WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Notification marked as read: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Error marking notification as read: {}", id, e);
            throw new RuntimeException("Failed to mark notification as read", e);
        }
    }

    public void markAllAsReadByUserId(Integer userId) {
        String sql = "UPDATE notifications SET is_read = true WHERE user_id = ? AND is_read = false";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("All notifications marked as read for user: {}", userId);
            }
        } catch (SQLException e) {
            logger.error("Error marking all notifications as read for user: {}", userId, e);
            throw new RuntimeException("Failed to mark all notifications as read", e);
        }
    }

    public Optional<Notification> findById(Integer id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding notification by id: {}", id, e);
        }
        return Optional.empty();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM notifications WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                logger.info("Notification deleted: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Error deleting notification: {}", id, e);
            throw new RuntimeException("Failed to delete notification", e);
        }
    }

    public int countUnreadByUserId(Integer userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting unread notifications for user: {}", userId, e);
        }
        return 0;
    }

    private Notification mapRowToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setContent(rs.getString("content"));
        notification.setLink(rs.getString("link"));
        notification.setIsRead(rs.getBoolean("is_read"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }
        return notification;
    }
}