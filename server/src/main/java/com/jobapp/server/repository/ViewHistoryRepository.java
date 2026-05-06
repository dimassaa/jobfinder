package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Vacancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ViewHistoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(ViewHistoryRepository.class);

    public void add(int userId, int vacancyId) {
        String sql = "INSERT INTO view_history (user_id, vacancy_id, viewed_at) VALUES (?, ?, ?) " +
                    "ON CONFLICT (user_id, vacancy_id) DO UPDATE SET viewed_at = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, vacancyId);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            logger.info("View history added: user {} vacancy {}", userId, vacancyId);
        } catch (SQLException e) {
            logger.error("Error adding view history: user {} vacancy {}", userId, vacancyId, e);
            throw new RuntimeException("Failed to add view history", e);
        }
    }

    public boolean exists(int userId, int vacancyId) {
        String sql = "SELECT 1 FROM view_history WHERE user_id = ? AND vacancy_id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, vacancyId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking view history exists: user {} vacancy {}", userId, vacancyId, e);
            return false;
        }
    }

    public void updateTimestamp(int userId, int vacancyId) {
        String sql = "UPDATE view_history SET viewed_at = ? WHERE user_id = ? AND vacancy_id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, userId);
            stmt.setInt(3, vacancyId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating view history timestamp: user {} vacancy {}", userId, vacancyId, e);
        }
    }

    public List<Vacancy> getRecentViews(int userId, int limit) {
        String sql = "SELECT v.* FROM vacancies v " +
                    "INNER JOIN view_history vh ON v.id = vh.vacancy_id " +
                    "WHERE vh.user_id = ? " +
                    "ORDER BY vh.viewed_at DESC " +
                    "LIMIT ?";
        List<Vacancy> vacancies = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vacancies.add(mapRowToVacancy(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching recent views for user: {}", userId, e);
        }
        return vacancies;
    }

    private Vacancy mapRowToVacancy(ResultSet rs) throws SQLException {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(rs.getInt("id"));
        vacancy.setCompanyId(rs.getInt("company_id"));
        vacancy.setTitle(rs.getString("title"));
        vacancy.setDescription(rs.getString("description"));
        vacancy.setCity(rs.getString("city"));

        int salaryMin = rs.getInt("salary_min");
        if (!rs.wasNull()) {
            vacancy.setSalaryMin(salaryMin);
        }
        int salaryMax = rs.getInt("salary_max");
        if (!rs.wasNull()) {
            vacancy.setSalaryMax(salaryMax);
        }

        vacancy.setEmploymentType(rs.getString("employment_type"));
        vacancy.setIsActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            vacancy.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp closedAt = rs.getTimestamp("closed_at");
        if (closedAt != null) {
            vacancy.setClosedAt(closedAt.toLocalDateTime());
        }
        return vacancy;
    }
}