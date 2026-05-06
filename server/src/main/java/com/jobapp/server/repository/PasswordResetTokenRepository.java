package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.PasswordResetToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class PasswordResetTokenRepository {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenRepository.class);

    private DataSource dataSource;

    public PasswordResetTokenRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public PasswordResetTokenRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(PasswordResetToken token) {
        String sql = "INSERT INTO password_reset_tokens (user_id, token, expires_at, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, token.getUserId());
            stmt.setString(2, token.getToken());
            setTimestampOrNull(stmt, 3, token.getExpiresAt());
            setTimestampOrNull(stmt, 4, token.getCreatedAt());
            stmt.executeUpdate();
            logger.info("Password reset token saved for user: {}", token.getUserId());
        } catch (SQLException e) {
            logger.error("Error saving password reset token for user: {}", token.getUserId(), e);
            throw new RuntimeException("Failed to save password reset token", e);
        }
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        String sql = "SELECT * FROM password_reset_tokens WHERE token = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToToken(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding password reset token: {}", token, e);
        }
        return Optional.empty();
    }

    public void deleteByUserId(int userId) {
        String sql = "DELETE FROM password_reset_tokens WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                logger.info("Password reset tokens deleted for user: {}", userId);
            }
        } catch (SQLException e) {
            logger.error("Error deleting password reset tokens for user: {}", userId, e);
            throw new RuntimeException("Failed to delete password reset tokens", e);
        }
    }

    public void deleteByToken(String token) {
        String sql = "DELETE FROM password_reset_tokens WHERE token = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
            logger.info("Password reset token deleted: {}", token);
        } catch (SQLException e) {
            logger.error("Error deleting password reset token: {}", token, e);
            throw new RuntimeException("Failed to delete password reset token", e);
        }
    }

    public boolean deleteByTokenIfValid(String token) {
        String selectSql = "SELECT id FROM password_reset_tokens WHERE token = ? AND expires_at > NOW()";
        String deleteSql = "DELETE FROM password_reset_tokens WHERE token = ?";
        boolean originalAutoCommit;
        try (Connection conn = dataSource.getConnection()) {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, token);
                    var rs = selectStmt.executeQuery();
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                }
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, token);
                    deleteStmt.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            logger.error("Error deleting password reset token: {}", token, e);
            throw new RuntimeException("Failed to delete password reset token", e);
        }
    }

    public boolean hasActiveToken(int userId) {
        String sql = "SELECT 1 FROM password_reset_tokens WHERE user_id = ? AND expires_at > NOW()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking active token for user: {}", userId, e);
            return false;
        }
    }

    private void setTimestampOrNull(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value != null) {
            stmt.setTimestamp(index, Timestamp.valueOf(value));
        } else {
            stmt.setNull(index, Types.TIMESTAMP);
        }
    }

    private PasswordResetToken mapRowToToken(ResultSet rs) throws SQLException {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(rs.getInt("id"));
        token.setUserId(rs.getInt("user_id"));
        token.setToken(rs.getString("token"));
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            token.setExpiresAt(expiresAt.toLocalDateTime());
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            token.setCreatedAt(createdAt.toLocalDateTime());
        }
        return token;
    }
}