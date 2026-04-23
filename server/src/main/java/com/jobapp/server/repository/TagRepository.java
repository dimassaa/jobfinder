package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TagRepository {

    private static final Logger logger = LoggerFactory.getLogger(TagRepository.class);

    public List<Tag> findAll() {
        String sql = "SELECT * FROM tags ORDER BY name";
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tags.add(mapRowToTag(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all tags", e);
        }
        return tags;
    }

    public Optional<Tag> findById(Integer id) {
        String sql = "SELECT * FROM tags WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTag(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding tag by id: {}", id, e);
        }
        return Optional.empty();
    }

    public Tag save(Tag tag) {
        String sql = "INSERT INTO tags (name, is_active, created_at) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tag.getName());
            stmt.setBoolean(2, tag.getIsActive() != null ? tag.getIsActive() : true);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tag.setId(rs.getInt("id"));
                }
            }
            logger.info("Tag saved with id: {}", tag.getId());
            return tag;
        } catch (SQLException e) {
            logger.error("Error saving tag: {}", tag.getName(), e);
            throw new RuntimeException("Failed to save tag", e);
        }
    }

    public void updateActiveStatus(Integer id, boolean isActive) {
        String sql = "UPDATE tags SET is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, id);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Tag active status updated: {} -> {}", id, isActive);
            }
        } catch (SQLException e) {
            logger.error("Error updating tag active status: {}", id, e);
            throw new RuntimeException("Failed to update tag active status", e);
        }
    }

    public Optional<Tag> findByName(String name) {
        String sql = "SELECT * FROM tags WHERE name = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTag(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding tag by name: {}", name, e);
        }
        return Optional.empty();
    }

    public List<Tag> findByVacancyId(int vacancyId) {
        String sql = "SELECT t.* FROM tags t " +
                    "JOIN vacancy_tags vt ON t.id = vt.tag_id " +
                    "WHERE vt.vacancy_id = ? " +
                    "ORDER BY t.name";
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vacancyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapRowToTag(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding tags by vacancy id: {}", vacancyId, e);
        }
        return tags;
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