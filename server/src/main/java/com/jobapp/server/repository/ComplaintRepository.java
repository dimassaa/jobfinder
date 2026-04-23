package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Complaint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ComplaintRepository {

    private static final Logger logger = LoggerFactory.getLogger(ComplaintRepository.class);

    public Complaint save(Complaint complaint) {
        String sql = "INSERT INTO complaints (user_id, target_type, target_id, reason, status, created_at, resolved_by, resolved_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, complaint.getUserId());
            stmt.setString(2, complaint.getTargetType());
            stmt.setInt(3, complaint.getTargetId());
            stmt.setString(4, complaint.getReason());
            stmt.setString(5, complaint.getStatus() != null ? complaint.getStatus() : "PENDING");
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            if (complaint.getResolvedBy() != null) {
                stmt.setInt(7, complaint.getResolvedBy());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            if (complaint.getResolvedAt() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(complaint.getResolvedAt()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    complaint.setId(rs.getInt("id"));
                }
            }
            logger.info("Complaint saved with id: {}", complaint.getId());
            return complaint;
        } catch (SQLException e) {
            logger.error("Error saving complaint for user: {}", complaint.getUserId(), e);
            throw new RuntimeException("Failed to save complaint", e);
        }
    }

    public List<Complaint> findAll() {
        String sql = "SELECT * FROM complaints ORDER BY created_at DESC";
        List<Complaint> complaints = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                complaints.add(mapRowToComplaint(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all complaints", e);
        }
        return complaints;
    }

    public List<Complaint> findAllPending() {
        String sql = "SELECT * FROM complaints WHERE status = 'PENDING' ORDER BY created_at DESC";
        List<Complaint> complaints = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                complaints.add(mapRowToComplaint(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching pending complaints", e);
        }
        return complaints;
    }

    public Optional<Complaint> findById(Integer id) {
        String sql = "SELECT * FROM complaints WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToComplaint(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding complaint by id: {}", id, e);
        }
        return Optional.empty();
    }

    public List<Complaint> findByUserId(Integer userId) {
        String sql = "SELECT * FROM complaints WHERE user_id = ? ORDER BY created_at DESC";
        List<Complaint> complaints = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    complaints.add(mapRowToComplaint(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching complaints by user id: {}", userId, e);
        }
        return complaints;
    }

    public List<Complaint> findByStatus(String status) {
        String sql = "SELECT * FROM complaints WHERE status = ? ORDER BY created_at DESC";
        List<Complaint> complaints = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    complaints.add(mapRowToComplaint(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching complaints by status: {}", status, e);
        }
        return complaints;
    }

    public void updateStatus(Integer id, String status, Integer resolvedBy) {
        updateStatus((int) id, status, (int) resolvedBy);
    }

    public void updateStatus(int id, String status, int resolvedBy) {
        String sql = "UPDATE complaints SET status = ?, resolved_by = ?, resolved_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, resolvedBy);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, id);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Complaint status updated: {} -> {}", id, status);
            }
        } catch (SQLException e) {
            logger.error("Error updating complaint status: {}", id, e);
            throw new RuntimeException("Failed to update complaint status", e);
        }
    }

    private Complaint mapRowToComplaint(ResultSet rs) throws SQLException {
        Complaint complaint = new Complaint();
        complaint.setId(rs.getInt("id"));
        complaint.setUserId(rs.getInt("user_id"));
        complaint.setTargetType(rs.getString("target_type"));
        complaint.setTargetId(rs.getInt("target_id"));
        complaint.setReason(rs.getString("reason"));
        complaint.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            complaint.setCreatedAt(createdAt.toLocalDateTime());
        }

        int resolvedBy = rs.getInt("resolved_by");
        if (!rs.wasNull()) {
            complaint.setResolvedBy(resolvedBy);
        }

        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        if (resolvedAt != null) {
            complaint.setResolvedAt(resolvedAt.toLocalDateTime());
        }
        return complaint;
    }
}