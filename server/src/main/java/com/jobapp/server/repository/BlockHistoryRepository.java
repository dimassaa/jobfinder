package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.BlockHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class BlockHistoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(BlockHistoryRepository.class);

    private DataSource dataSource;

    public BlockHistoryRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public BlockHistoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(BlockHistory entry) {
        String sql = "INSERT INTO block_history (user_id, blocked_by, reason, blocked_until, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, entry.getUserId());
            stmt.setInt(2, entry.getBlockedBy());
            setStringOrNull(stmt, 3, entry.getReason());
            setTimestampOrNull(stmt, 4, entry.getBlockedUntil());
            setTimestampOrNull(stmt, 5, entry.getCreatedAt());
            stmt.executeUpdate();
            logger.info("Block history saved for user: {} by admin: {}", entry.getUserId(), entry.getBlockedBy());
        } catch (SQLException e) {
            logger.error("Error saving block history for user: {}", entry.getUserId(), e);
            throw new RuntimeException("Failed to save block history", e);
        }
    }

    public List<BlockHistory> findByUserId(int userId) {
        String sql = "SELECT * FROM block_history WHERE user_id = ? ORDER BY created_at DESC";
        List<BlockHistory> entries = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRowToBlockHistory(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding block history for user: {}", userId, e);
        }
        return entries;
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

    private BlockHistory mapRowToBlockHistory(ResultSet rs) throws SQLException {
        BlockHistory entry = new BlockHistory();
        entry.setId(rs.getInt("id"));
        entry.setUserId(rs.getInt("user_id"));
        entry.setBlockedBy(rs.getInt("blocked_by"));
        entry.setReason(rs.getString("reason"));
        Timestamp blockedUntil = rs.getTimestamp("blocked_until");
        if (blockedUntil != null) {
            entry.setBlockedUntil(blockedUntil.toLocalDateTime());
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            entry.setCreatedAt(createdAt.toLocalDateTime());
        }
        return entry;
    }
}