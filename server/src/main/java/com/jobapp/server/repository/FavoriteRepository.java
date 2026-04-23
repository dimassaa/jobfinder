package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Vacancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FavoriteRepository {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteRepository.class);

    public void addFavorite(Integer userId, Integer vacancyId) {
        String sql = "INSERT INTO favorites (user_id, vacancy_id, created_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, vacancyId);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            logger.info("Favorite added: user {} vacancy {}", userId, vacancyId);
        } catch (SQLException e) {
            logger.error("Error adding favorite: user {} vacancy {}", userId, vacancyId, e);
            throw new RuntimeException("Failed to add favorite", e);
        }
    }

    public void removeFavorite(Integer userId, Integer vacancyId) {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND vacancy_id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, vacancyId);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                logger.info("Favorite removed: user {} vacancy {}", userId, vacancyId);
            }
        } catch (SQLException e) {
            logger.error("Error removing favorite: user {} vacancy {}", userId, vacancyId, e);
            throw new RuntimeException("Failed to remove favorite", e);
        }
    }

    public List<Vacancy> findByUserId(Integer userId) {
        return findFavoritesByUser(userId);
    }

    public void add(int userId, int vacancyId) {
        addFavorite(userId, vacancyId);
    }

    public void remove(int userId, int vacancyId) {
        removeFavorite(userId, vacancyId);
    }

    public List<Vacancy> findFavoritesByUser(int userId) {
        String sql = "SELECT v.* FROM vacancies v INNER JOIN favorites f ON v.id = f.vacancy_id WHERE f.user_id = ? ORDER BY f.created_at DESC";
        List<Vacancy> vacancies = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vacancies.add(mapRowToVacancy(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching favorites by user id: {}", userId, e);
        }
        return vacancies;
    }

    public boolean exists(int userId, int vacancyId) {
        return isFavorite(userId, vacancyId);
    }

    public List<Vacancy> findFavoritesByUser(Integer userId) {
        String sql = "SELECT v.* FROM vacancies v INNER JOIN favorites f ON v.id = f.vacancy_id WHERE f.user_id = ? ORDER BY f.created_at DESC";
        List<Vacancy> vacancies = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vacancies.add(mapRowToVacancy(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching favorites by user id: {}", userId, e);
        }
        return vacancies;
    }

    public boolean isFavorite(Integer userId, Integer vacancyId) {
        String sql = "SELECT 1 FROM favorites WHERE user_id = ? AND vacancy_id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, vacancyId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking favorite: user {} vacancy {}", userId, vacancyId, e);
        }
        return false;
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