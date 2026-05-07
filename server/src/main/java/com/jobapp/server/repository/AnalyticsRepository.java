package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.common.dto.ChartDataPointDto;
import com.jobapp.common.dto.TagUsageDto;
import com.jobapp.common.dto.CityCountDto;
import com.jobapp.common.dto.ConversionStatsDto;
import com.jobapp.common.dto.SystemStatsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsRepository {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsRepository.class);

    private DataSource dataSource;

    public AnalyticsRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public AnalyticsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<ChartDataPointDto> getSalaryStatsByUserTags(int userId, int days) {
        List<ChartDataPointDto> result = new ArrayList<>();
        String sql = "SELECT DATE(v.created_at) as date, AVG((v.salary_min + v.salary_max)/2.0) as avg_salary " +
                    "FROM vacancies v " +
                    "JOIN vacancy_tags vt ON v.id = vt.vacancy_id " +
                    "JOIN user_tags ut ON vt.tag_id = ut.tag_id " +
                    "WHERE ut.user_id = ? AND v.created_at >= NOW() - (? || ' days')::interval " +
                    "AND v.salary_min IS NOT NULL AND v.salary_max IS NOT NULL " +
                    "GROUP BY DATE(v.created_at) " +
                    "ORDER BY date";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, days);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date date = rs.getDate("date");
                    double avgSalary = rs.getDouble("avg_salary");
                    if (!rs.wasNull()) {
                        result.add(new ChartDataPointDto(date != null ? date.toString() : null, avgSalary));
                    }
                }
            }
            logger.debug("Salary stats by user tags returned {} points", result.size());
        } catch (SQLException e) {
            logger.error("Error getting salary stats by user tags for userId: {}", userId, e);
            throw new RuntimeException("Failed to get salary stats", e);
        }
        return result;
    }

    public List<TagUsageDto> getTopTags(int days, int limit) {
        List<TagUsageDto> result = new ArrayList<>();
        String sql = "SELECT t.id, t.name, COUNT(vt.vacancy_id) as count " +
                    "FROM vacancy_tags vt " +
                    "JOIN tags t ON vt.tag_id = t.id " +
                    "JOIN vacancies v ON vt.vacancy_id = v.id " +
                    "WHERE v.created_at >= NOW() - (? || ' days')::interval " +
                    "GROUP BY t.id, t.name " +
                    "ORDER BY count DESC " +
                    "LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    int count = rs.getInt("count");
                    result.add(new TagUsageDto(id, name, count));
                }
            }
            logger.debug("Top tags returned {} results", result.size());
        } catch (SQLException e) {
            logger.error("Error getting top tags", e);
            throw new RuntimeException("Failed to get top tags", e);
        }
        return result;
    }

    public List<CityCountDto> getGeoDistribution(String entityType, int days) {
        List<CityCountDto> result = new ArrayList<>();
        String sql;
        if ("VACANCY".equalsIgnoreCase(entityType)) {
            sql = "SELECT city, COUNT(*) as count, AVG((salary_min+salary_max)/2.0) as avg_salary " +
                 "FROM vacancies " +
                 "WHERE created_at >= NOW() - (? || ' days')::interval AND city IS NOT NULL " +
                 "GROUP BY city " +
                 "ORDER BY count DESC";
        } else {
            sql = "SELECT city, COUNT(*) as count, NULL as avg_salary " +
                 "FROM users " +
                 "WHERE role = 'USER' AND created_at >= NOW() - (? || ' days')::interval AND city IS NOT NULL " +
                 "GROUP BY city " +
                 "ORDER BY count DESC";
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String city = rs.getString("city");
                    int count = rs.getInt("count");
                    Double avgSalary = rs.getDouble("avg_salary");
                    if (rs.wasNull()) {
                        avgSalary = null;
                    }
                    result.add(new CityCountDto(city, count, avgSalary));
                }
            }
            logger.debug("Geo distribution for {} returned {} results", entityType, result.size());
        } catch (SQLException e) {
            logger.error("Error getting geo distribution for entity type: {}", entityType, e);
            throw new RuntimeException("Failed to get geo distribution", e);
        }
        return result;
    }

    public ConversionStatsDto getVacancyConversion(int vacancyId) {
        String responsesSql = "SELECT COUNT(*) FROM applications WHERE vacancy_id = ?";
        String invitesSql = "SELECT COUNT(*) FROM applications WHERE vacancy_id = ? AND status IN ('INVITED', 'ACCEPTED')";

        int responses = 0;
        int invites = 0;

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(responsesSql)) {
                stmt.setInt(1, vacancyId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        responses = rs.getInt(1);
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(invitesSql)) {
                stmt.setInt(1, vacancyId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        invites = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting vacancy conversion for vacancyId: {}", vacancyId, e);
            throw new RuntimeException("Failed to get vacancy conversion", e);
        }

        double conversionRate = responses > 0 ? (double) invites / responses * 100.0 : 0.0;
        return new ConversionStatsDto(0, responses, invites, conversionRate);
    }

    public SystemStatsDto getSystemStats() {
        String usersSql = "SELECT COUNT(*) FROM users";
        String applicantsSql = "SELECT COUNT(*) FROM users WHERE role = 'USER'";
        String hrSql = "SELECT COUNT(*) FROM users WHERE role = 'HR'";
        String adminsSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        String vacanciesSql = "SELECT COUNT(*) FROM vacancies WHERE is_active = true";
        String applicationsSql = "SELECT COUNT(*) FROM applications";

        int totalUsers = 0;
        int totalApplicants = 0;
        int totalHr = 0;
        int totalAdmins = 0;
        int totalVacancies = 0;
        int totalApplications = 0;

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(usersSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalUsers = rs.getInt(1);
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(applicantsSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalApplicants = rs.getInt(1);
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(hrSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalHr = rs.getInt(1);
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(adminsSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalAdmins = rs.getInt(1);
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(vacanciesSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalVacancies = rs.getInt(1);
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(applicationsSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalApplications = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting system stats", e);
            throw new RuntimeException("Failed to get system stats", e);
        }

        logger.debug("System stats: users={}, applicants={}, hr={}, admins={}, vacancies={}, applications={}",
                totalUsers, totalApplicants, totalHr, totalAdmins, totalVacancies, totalApplications);
        return new SystemStatsDto(totalUsers, totalApplicants, totalHr, totalAdmins, totalVacancies, totalApplications);
    }

    public List<ChartDataPointDto> getSalaryStatsByTagIds(List<Integer> tagIds) {
        List<ChartDataPointDto> result = new ArrayList<>();
        if (tagIds == null || tagIds.isEmpty()) {
            return result;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.name as tag_name, AVG((v.salary_min + v.salary_max)/2.0) as avg_salary ");
        sql.append("FROM vacancies v ");
        sql.append("JOIN vacancy_tags vt ON v.id = vt.vacancy_id ");
        sql.append("JOIN tags t ON vt.tag_id = t.id ");
        sql.append("WHERE vt.tag_id IN (");

        for (int i = 0; i < tagIds.size(); i++) {
            sql.append("?");
            if (i < tagIds.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(") AND v.salary_min IS NOT NULL AND v.salary_max IS NOT NULL ");
        sql.append("GROUP BY t.id, t.name ");
        sql.append("ORDER BY avg_salary DESC");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < tagIds.size(); i++) {
                stmt.setInt(i + 1, tagIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tagName = rs.getString("tag_name");
                    double avgSalary = rs.getDouble("avg_salary");
                    if (!rs.wasNull()) {
                        result.add(new ChartDataPointDto(tagName, avgSalary));
                    }
                }
            }
            logger.debug("Salary stats by tag ids returned {} points", result.size());
        } catch (SQLException e) {
            logger.error("Error getting salary stats by tag ids", e);
            throw new RuntimeException("Failed to get salary stats by tag ids", e);
        }
        return result;
    }

    public List<ChartDataPointDto> getActivityTimeline(int days) {
        List<ChartDataPointDto> result = new ArrayList<>();
        String sql = "SELECT DATE(created_at) as date, COUNT(*) as count " +
                    "FROM users " +
                    "WHERE created_at >= NOW() - (? || ' days')::interval " +
                    "GROUP BY DATE(created_at) " +
                    "ORDER BY date";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date date = rs.getDate("date");
                    double count = rs.getDouble("count");
                    if (!rs.wasNull()) {
                        result.add(new ChartDataPointDto(date != null ? date.toString() : null, count));
                    }
                }
            }
            logger.debug("Activity timeline returned {} points", result.size());
        } catch (SQLException e) {
            logger.error("Error getting activity timeline", e);
            throw new RuntimeException("Failed to get activity timeline", e);
        }
        return result;
    }

    public List<ChartDataPointDto> getAllSalaryStats(int days) {
        List<ChartDataPointDto> result = new ArrayList<>();
        String sql = "SELECT DATE(v.created_at) as date, AVG((v.salary_min + v.salary_max)/2.0) as avg_salary " +
                    "FROM vacancies v " +
                    "WHERE v.created_at >= NOW() - (? || ' days')::interval " +
                    "AND v.salary_min IS NOT NULL AND v.salary_max IS NOT NULL " +
                    "GROUP BY DATE(v.created_at) " +
                    "ORDER BY date";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date date = rs.getDate("date");
                    double avgSalary = rs.getDouble("avg_salary");
                    if (!rs.wasNull()) {
                        result.add(new ChartDataPointDto(date != null ? date.toString() : null, avgSalary));
                    }
                }
            }
            logger.debug("All salary stats returned {} points", result.size());
        } catch (SQLException e) {
            logger.error("Error getting all salary stats", e);
            throw new RuntimeException("Failed to get all salary stats", e);
        }
        return result;
    }
}