package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private DataSource dataSource;

    public UserRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by email: {}", email, e);
        }
        return Optional.empty();
    }

    public User save(User user) {
        String sql = "INSERT INTO users (email, password_hash, role, name, surname, city, phone, additional_info, is_blocked, blocked_until, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole());
            setStringOrNull(stmt, 4, user.getName());
            setStringOrNull(stmt, 5, user.getSurname());
            setStringOrNull(stmt, 6, user.getCity());
            setStringOrNull(stmt, 7, user.getPhone());
            setStringOrNull(stmt, 8, user.getAdditionalInfo());
            stmt.setBoolean(9, user.getIsBlocked() != null ? user.getIsBlocked() : false);
            setTimestampOrNull(stmt, 10, user.getBlockedUntil());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getInt("id"));
                }
            }
            logger.info("User saved with id: {}", user.getId());
            return user;
        } catch (SQLException e) {
            logger.error("Error saving user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to save user", e);
        }
    }

    public void update(User user) {
        String sql = "UPDATE users SET email = ?, password_hash = ?, role = ?, name = ?, surname = ?, city = ?, phone = ?, " +
                    "additional_info = ?, is_blocked = ?, blocked_until = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getName());
            stmt.setString(5, user.getSurname());
            stmt.setString(6, user.getCity());
            stmt.setString(7, user.getPhone());
            stmt.setString(8, user.getAdditionalInfo());
            stmt.setBoolean(9, user.getIsBlocked() != null ? user.getIsBlocked() : false);
            stmt.setTimestamp(10, user.getBlockedUntil() != null ? Timestamp.valueOf(user.getBlockedUntil()) : null);
            stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(12, user.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("User updated: {}", user.getId());
            }
        } catch (SQLException e) {
            logger.error("Error updating user: {}", user.getId(), e);
            throw new RuntimeException("Failed to update user", e);
        }
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id: {}", id, e);
        }
        return Optional.empty();
    }

    public void delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                logger.info("User deleted: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Error deleting user: {}", id, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking if user exists: {}", email, e);
        }
        return false;
    }

    public void updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, userId);
            stmt.executeUpdate();
            logger.info("Password updated for user: {}", userId);
        } catch (SQLException e) {
            logger.error("Error updating password for user: {}", userId, e);
            throw new RuntimeException("Failed to update password", e);
        }
    }

    public void updateProfile(int userId, String name, String surname, String city, String phone, String additionalInfo) {
        String sql = "UPDATE users SET name = ?, surname = ?, city = ?, phone = ?, additional_info = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setStringOrNull(stmt, 1, name);
            setStringOrNull(stmt, 2, surname);
            setStringOrNull(stmt, 3, city);
            setStringOrNull(stmt, 4, phone);
            setStringOrNull(stmt, 5, additionalInfo);
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(7, userId);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Profile updated for user: {}", userId);
            }
        } catch (SQLException e) {
            logger.error("Error updating profile for user: {}", userId, e);
            throw new RuntimeException("Failed to update profile", e);
        }
    }

    public List<Tag> findUserTags(int userId) {
        String sql = "SELECT t.* FROM tags t " +
                     "INNER JOIN user_tags ut ON t.id = ut.tag_id " +
                     "WHERE ut.user_id = ? ORDER BY t.name";
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapRowToTag(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user tags for user: {}", userId, e);
        }
        return tags;
    }

    public void updateUserTags(int userId, List<Integer> tagIds) {
        String deleteSql = "DELETE FROM user_tags WHERE user_id = ?";
        String insertSql = "INSERT INTO user_tags (user_id, tag_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                deleteStmt.setInt(1, userId);
                deleteStmt.executeUpdate();
                for (Integer tagId : tagIds) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setInt(2, tagId);
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
                conn.commit();
                logger.info("User tags updated for user: {}", userId);
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Error updating user tags for user: {}", userId, e);
                throw new RuntimeException("Failed to update user tags", e);
            }
        } catch (SQLException e) {
            logger.error("Error updating user tags for user: {}", userId, e);
            throw new RuntimeException("Failed to update user tags", e);
        }
    }

    private void setStringOrNull(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value != null) {
            stmt.setString(index, value);
        } else {
            stmt.setNull(index, Types.VARCHAR);
        }
    }

    private void setTimestampOrNull(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value != null) {
            stmt.setTimestamp(index, Timestamp.valueOf(value));
        } else {
            stmt.setNull(index, Types.TIMESTAMP);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setName(rs.getString("name"));
        user.setSurname(rs.getString("surname"));
        user.setCity(rs.getString("city"));
        user.setPhone(rs.getString("phone"));
        user.setAdditionalInfo(rs.getString("additional_info"));
        user.setIsBlocked(rs.getBoolean("is_blocked"));
        Timestamp blockedUntil = rs.getTimestamp("blocked_until");
        if (blockedUntil != null) {
            user.setBlockedUntil(blockedUntil.toLocalDateTime());
        }
        user.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        user.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return user;
    }

    private Tag mapRowToTag(ResultSet rs) throws SQLException {
        Tag tag = new Tag();
        tag.setId(rs.getInt("id"));
        tag.setName(rs.getString("name"));
        tag.setIsActive(rs.getBoolean("is_active"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            tag.setCreatedAt(createdAt.toLocalDateTime());
        }
        return tag;
    }
}