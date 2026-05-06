package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;

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

    public Map<Integer, List<Tag>> findByVacancyIds(Collection<Integer> vacancyIds) {
        Map<Integer, List<Tag>> result = new HashMap<>();
        if (vacancyIds == null || vacancyIds.isEmpty()) {
            return result;
        }
        String sql = "SELECT t.*, vt.vacancy_id as v_id FROM tags t " +
                    "JOIN vacancy_tags vt ON t.id = vt.tag_id " +
                    "WHERE vt.vacancy_id IN (";
        for (int i = 0; i < vacancyIds.size(); i++) {
            sql += "?";
            if (i < vacancyIds.size() - 1) sql += ",";
        }
        sql += ") ORDER BY t.name";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer id : vacancyIds) {
                stmt.setInt(idx++, id);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Tag tag = mapRowToTag(rs);
                    int vacancyId = rs.getInt("v_id");
                    result.computeIfAbsent(vacancyId, k -> new ArrayList<>()).add(tag);
                }
            }
            logger.debug("Found tags for {} vacancies", result.size());
        } catch (SQLException e) {
            logger.error("Error finding tags by vacancy ids", e);
        }
        return result;
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

    public Map<Integer, Tag> findByIds(Collection<Integer> ids) {
        Map<Integer, Tag> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM tags WHERE id IN (");
        for (int i = 0; i < ids.size(); i++) {
            sql.append("?");
            if (i < ids.size() - 1) sql.append(",");
        }
        sql.append(")");
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (Integer id : ids) {
                stmt.setInt(idx++, id);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Tag tag = mapRowToTag(rs);
                    result.put(tag.getId(), tag);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding tags by ids", e);
        }
        return result;
    }
}