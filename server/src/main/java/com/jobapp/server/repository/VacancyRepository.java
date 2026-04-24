package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.model.VacancyWithTags;
import com.jobapp.common.dto.VacancyFilterDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class VacancyRepository {

    private static final Logger logger = LoggerFactory.getLogger(VacancyRepository.class);

    private DataSource dataSource;

    public VacancyRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public VacancyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Vacancy save(Vacancy vacancy) {
        String sql = "INSERT INTO vacancies (company_id, title, description, city, salary_min, salary_max, employment_type, is_active, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection();
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

    public List<Vacancy> search(VacancyFilterDto filter) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT v.* FROM vacancies v WHERE v.is_active = true");
        List<Object> params = new ArrayList<>();

        if (filter.getCity() != null && !filter.getCity().isBlank()) {
            sql.append(" AND LOWER(v.city) = LOWER(?)");
            params.add(filter.getCity().trim());
        }

        if (filter.getSalaryFrom() != null) {
            sql.append(" AND v.salary_max >= ?");
            params.add(filter.getSalaryFrom());
        }

        if (filter.getSalaryTo() != null) {
            sql.append(" AND v.salary_min <= ?");
            params.add(filter.getSalaryTo());
        }

        if (filter.getEmploymentType() != null && !filter.getEmploymentType().isBlank()) {
            sql.append(" AND v.employment_type = ?");
            params.add(filter.getEmploymentType());
        }

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            String keywordPattern = "%" + filter.getKeyword().trim() + "%";
            sql.append(" AND (v.title ILIKE ? OR v.description ILIKE ?)");
            params.add(keywordPattern);
            params.add(keywordPattern);
        }

        if (filter.getCreatedAfter() != null) {
            sql.append(" AND v.created_at >= ?");
            params.add(Timestamp.valueOf(filter.getCreatedAfter()));
        }

        if (filter.getTagIds() != null && !filter.getTagIds().isEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM vacancy_tags vt WHERE vt.vacancy_id = v.id AND vt.tag_id IN (");
            for (int i = 0; i < filter.getTagIds().size(); i++) {
                sql.append("?");
                if (i < filter.getTagIds().size() - 1) {
                    sql.append(",");
                }
                params.add(filter.getTagIds().get(i));
            }
            sql.append("))");
        }

        sql.append(" ORDER BY v.created_at DESC");

        List<Vacancy> vacancies = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Timestamp) {
                    stmt.setTimestamp(i + 1, (Timestamp) param);
                }
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vacancies.add(mapRowToVacancy(rs));
                }
            }
            logger.debug("Search returned {} vacancies", vacancies.size());
        } catch (SQLException e) {
            logger.error("Error searching vacancies with filter", e);
        }
        return vacancies;
    }

    public void addTags(int vacancyId, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO vacancy_tags (vacancy_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Integer tagId : tagIds) {
                stmt.setInt(1, vacancyId);
                stmt.setInt(2, tagId);
                stmt.addBatch();
            }
            stmt.executeBatch();
            logger.info("Added {} tags to vacancy {}", tagIds.size(), vacancyId);
        } catch (SQLException e) {
            logger.error("Error adding tags to vacancy: {}", vacancyId, e);
            throw new RuntimeException("Failed to add tags to vacancy", e);
        }
    }

    public void clearTags(int vacancyId) {
        String sql = "DELETE FROM vacancy_tags WHERE vacancy_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vacancyId);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                logger.info("Cleared {} tags from vacancy {}", rowsDeleted, vacancyId);
            }
        } catch (SQLException e) {
            logger.error("Error clearing tags from vacancy: {}", vacancyId, e);
            throw new RuntimeException("Failed to clear tags from vacancy", e);
        }
    }

    public List<VacancyWithTags> findAllActiveWithTags() {
        List<VacancyWithTags> result = new ArrayList<>();
        String sql = "SELECT v.id, vt.tag_id FROM vacancies v " +
                    "LEFT JOIN vacancy_tags vt ON v.id = vt.vacancy_id " +
                    "WHERE v.is_active = true";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            Map<Integer, Set<Integer>> vacancyTagsMap = new HashMap<>();
            while (rs.next()) {
                int vacancyId = rs.getInt("id");
                int tagId = rs.getInt("tag_id");
                Set<Integer> tags = vacancyTagsMap.computeIfAbsent(vacancyId, k -> new HashSet<>());
                if (!rs.wasNull()) {
                    tags.add(tagId);
                }
            }
            for (Integer vacancyId : vacancyTagsMap.keySet()) {
                result.add(new VacancyWithTags(vacancyId, vacancyTagsMap.get(vacancyId)));
            }
            logger.debug("Found {} active vacancies with tags", result.size());
        } catch (SQLException e) {
            logger.error("Error fetching active vacancies with tags", e);
            throw new RuntimeException("Failed to fetch active vacancies with tags", e);
        }
        return result;
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