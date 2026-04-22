package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Vacancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VacancyRepository {

    private static final Logger logger = LoggerFactory.getLogger(VacancyRepository.class);

    public Vacancy save(Vacancy vacancy) {
        String sql = "INSERT INTO vacancies (company_id, title, description, city, salary_min, salary_max, employment_type, is_active, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vacancy.getCompanyId());
            stmt.setString(2, vacancy.getTitle());
            stmt.setString(3, vacancy.getDescription());
            stmt.setString(4, vacancy.getCity());
            stmt.setObject(5, vacancy.getSalaryMin());
            stmt.setObject(6, vacancy.getSalaryMax());
            stmt.setString(7, vacancy.getEmploymentType());
            stmt.setBoolean(8, vacancy.getIsActive() != null ? vacancy.getIsActive() : true);
            stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    vacancy.setId(rs.getInt("id"));
                }
            }
            logger.info("Vacancy saved with id: {}", vacancy.getId());
            return vacancy;
        } catch (SQLException e) {
            logger.error("Error saving vacancy: {}", vacancy.getTitle(), e);
            throw new RuntimeException("Failed to save vacancy", e);
        }
    }

    public Optional<Vacancy> findById(Integer id) {
        String sql = "SELECT * FROM vacancies WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToVacancy(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding vacancy by id: {}", id, e);
        }
        return Optional.empty();
    }

    public List<Vacancy> findAllActive() {
        String sql = "SELECT * FROM vacancies WHERE is_active = true ORDER BY created_at DESC";
        List<Vacancy> vacancies = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                vacancies.add(mapRowToVacancy(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching active vacancies", e);
        }
        return vacancies;
    }

    public List<Vacancy> findByCompanyId(Integer companyId) {
        String sql = "SELECT * FROM vacancies WHERE company_id = ? ORDER BY created_at DESC";
        List<Vacancy> vacancies = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, companyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vacancies.add(mapRowToVacancy(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching vacancies by company id: {}", companyId, e);
        }
        return vacancies;
    }

    public void update(Vacancy vacancy) {
        String sql = "UPDATE vacancies SET company_id = ?, title = ?, description = ?, city = ?, salary_min = ?, salary_max = ?, " +
                    "employment_type = ?, is_active = ?, closed_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vacancy.getCompanyId());
            stmt.setString(2, vacancy.getTitle());
            stmt.setString(3, vacancy.getDescription());
            stmt.setString(4, vacancy.getCity());
            stmt.setObject(5, vacancy.getSalaryMin());
            stmt.setObject(6, vacancy.getSalaryMax());
            stmt.setString(7, vacancy.getEmploymentType());
            stmt.setBoolean(8, vacancy.getIsActive() != null ? vacancy.getIsActive() : true);
            if (vacancy.getClosedAt() != null) {
                stmt.setTimestamp(9, Timestamp.valueOf(vacancy.getClosedAt()));
            } else {
                stmt.setNull(9, Types.TIMESTAMP);
            }
            stmt.setInt(10, vacancy.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Vacancy updated: {}", vacancy.getId());
            }
        } catch (SQLException e) {
            logger.error("Error updating vacancy: {}", vacancy.getId(), e);
            throw new RuntimeException("Failed to update vacancy", e);
        }
    }

    public List<Vacancy> search(String query) {
        String sql = "SELECT * FROM vacancies WHERE is_active = true AND " +
                    "(title ILIKE ? OR description ILIKE ? OR city ILIKE ?) ORDER BY created_at DESC";
        List<Vacancy> vacancies = new ArrayList<>();
        String searchPattern = "%" + query + "%";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vacancies.add(mapRowToVacancy(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching vacancies: {}", query, e);
        }
        return vacancies;
    }

    public void close(Integer id) {
        String sql = "UPDATE vacancies SET is_active = false, closed_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, id);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Vacancy closed: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Error closing vacancy: {}", id, e);
            throw new RuntimeException("Failed to close vacancy", e);
        }
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