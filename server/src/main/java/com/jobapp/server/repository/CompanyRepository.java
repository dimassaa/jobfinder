package com.jobapp.server.repository;

import com.jobapp.server.db.DatabaseConfig;
import com.jobapp.server.model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;

public class CompanyRepository {

    private static final Logger logger = LoggerFactory.getLogger(CompanyRepository.class);

    private DataSource dataSource;

    public CompanyRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public CompanyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Company save(Company company) {
        String sql = "INSERT INTO companies (user_id, company_name, website, description, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, company.getUserId());
            stmt.setString(2, company.getCompanyName());
            stmt.setString(3, company.getWebsite());
            stmt.setString(4, company.getDescription());
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            stmt.setTimestamp(5, now);
            stmt.setTimestamp(6, now);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    company.setId(rs.getInt("id"));
                }
            }
            logger.info("Company saved with id: {}", company.getId());
            return company;
        } catch (SQLException e) {
            logger.error("Error saving company: {}", company.getCompanyName(), e);
            throw new RuntimeException("Failed to save company", e);
        }
    }

    public Optional<Company> findById(Integer id) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCompany(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding company by id: {}", id, e);
        }
        return Optional.empty();
    }

    public Map<Integer, Company> findByIds(Collection<Integer> ids) {
        Map<Integer, Company> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM companies WHERE id IN (");
        for (int i = 0; i < ids.size(); i++) {
            sql.append("?");
            if (i < ids.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(")");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (Integer id : ids) {
                stmt.setInt(idx++, id);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Company company = mapRowToCompany(rs);
                    result.put(company.getId(), company);
                }
            }
            logger.debug("Found {} companies for {} IDs", result.size(), ids.size());
        } catch (SQLException e) {
            logger.error("Error finding companies by ids", e);
        }
        return result;
    }

    public Optional<Company> findByUserId(Integer userId) {
        String sql = "SELECT * FROM companies WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCompany(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding company by user id: {}", userId, e);
        }
        return Optional.empty();
    }

    public Optional<Company> findByVacancyId(Integer vacancyId) {
        String sql = "SELECT c.* FROM companies c " +
                    "INNER JOIN vacancies v ON c.id = v.company_id " +
                    "WHERE v.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vacancyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCompany(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding company by vacancy id: {}", vacancyId, e);
        }
        return Optional.empty();
    }

    public List<Company> findAll() {
        String sql = "SELECT * FROM companies ORDER BY company_name";
        List<Company> companies = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                companies.add(mapRowToCompany(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all companies", e);
        }
        return companies;
    }

    public void update(Company company) {
        String sql = "UPDATE companies SET company_name = ?, website = ?, description = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, company.getCompanyName());
            stmt.setString(2, company.getWebsite());
            stmt.setString(3, company.getDescription());
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(5, company.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Company updated: {}", company.getId());
            }
        } catch (SQLException e) {
            logger.error("Error updating company: {}", company.getId(), e);
            throw new RuntimeException("Failed to update company", e);
        }
    }

    private Company mapRowToCompany(ResultSet rs) throws SQLException {
        Company company = new Company();
        company.setId(rs.getInt("id"));
        company.setUserId(rs.getInt("user_id"));
        company.setCompanyName(rs.getString("company_name"));
        company.setWebsite(rs.getString("website"));
        company.setDescription(rs.getString("description"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            company.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            company.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return company;
    }
}