package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApplicationRepository {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationRepository.class);

    private DataSource dataSource;

    public ApplicationRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public ApplicationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Application save(Application application) {
        String sql = "INSERT INTO applications (vacancy_id, user_id, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, application.getVacancyId());
            stmt.setInt(2, application.getUserId());
            stmt.setString(3, application.getStatus() != null ? application.getStatus() : "PENDING");
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            stmt.setTimestamp(4, now);
            stmt.setTimestamp(5, now);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    application.setId(rs.getInt("id"));
                }
            }
            logger.info("Application saved with id: {}", application.getId());
            return application;
        } catch (SQLException e) {
            logger.error("Error saving application for vacancy: {} user: {}", application.getVacancyId(), application.getUserId(), e);
            throw new RuntimeException("Failed to save application", e);
        }
    }

    public Optional<Application> findExisting(Integer userId, Integer vacancyId) {
        String sql = "SELECT * FROM applications WHERE user_id = ? AND vacancy_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, vacancyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToApplication(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding existing application: user {} vacancy {}", userId, vacancyId, e);
        }
        return Optional.empty();
    }

    public List<Application> findByVacancyIdWithUser(Integer vacancyId) {
        String sql = "SELECT a.*, u.name as user_name, u.surname as user_surname " +
                     "FROM applications a " +
                     "JOIN users u ON a.user_id = u.id " +
                     "WHERE a.vacancy_id = ? ORDER BY a.created_at DESC";
        List<Application> applications = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vacancyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Application app = mapRowToApplication(rs);
                    String userName = rs.getString("user_name");
                    String userSurname = rs.getString("user_surname");
                    app.setUserName(userName);
                    app.setUserSurname(userSurname);
                    applications.add(app);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching applications with user by vacancy id: {}", vacancyId, e);
        }
        return applications;
    }

    public int countByUserLastMinute(Integer userId) {
        String sql = "SELECT COUNT(*) FROM applications WHERE user_id = ? AND created_at > NOW() - INTERVAL '1 MINUTE'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting applications by user last minute: {}", userId, e);
        }
        return 0;
    }

    public Optional<Application> findById(Integer id) {
        String sql = "SELECT * FROM applications WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToApplication(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding application by id: {}", id, e);
        }
        return Optional.empty();
    }

    public List<Application> findByUserId(Integer userId) {
        String sql = "SELECT * FROM applications WHERE user_id = ? ORDER BY created_at DESC";
        List<Application> applications = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    applications.add(mapRowToApplication(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching applications by user id: {}", userId, e);
        }
        return applications;
    }

    public List<Application> findByVacancyIds(List<Integer> vacancyIds) {
        if (vacancyIds == null || vacancyIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT * FROM applications WHERE vacancy_id = ANY(?) ORDER BY created_at DESC";
        List<Application> applications = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            Array array = conn.createArrayOf("INTEGER", vacancyIds.toArray());
            stmt.setArray(1, array);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    applications.add(mapRowToApplication(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching applications by vacancy ids", e);
        }
        return applications;
    }

    public List<Application> findByVacancyId(Integer vacancyId) {
        String sql = "SELECT * FROM applications WHERE vacancy_id = ? ORDER BY created_at DESC";
        List<Application> applications = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vacancyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    applications.add(mapRowToApplication(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching applications by vacancy id: {}", vacancyId, e);
        }
        return applications;
    }

    public void updateStatus(Integer id, String status) {
        String sql = "UPDATE applications SET status = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, id);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Application status updated: {} -> {}", id, status);
            }
        } catch (SQLException e) {
            logger.error("Error updating application status: {}", id, e);
            throw new RuntimeException("Failed to update application status", e);
        }
    }

    public boolean existsByUserAndVacancy(Integer userId, Integer vacancyId) {
        String sql = "SELECT 1 FROM applications WHERE user_id = ? AND vacancy_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, vacancyId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking application existence: user {} vacancy {}", userId, vacancyId, e);
        }
        return false;
    }

    private Application mapRowToApplication(ResultSet rs) throws SQLException {
        Application application = new Application();
        application.setId(rs.getInt("id"));
        application.setVacancyId(rs.getInt("vacancy_id"));
        application.setUserId(rs.getInt("user_id"));
        application.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            application.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            application.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return application;
    }
}