package com.jobapp.server.repository;

import com.jobapp.server.model.Company;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CompanyRepositoryTest {

    private CompanyRepository companyRepository;
    private HikariDataSource mockDataSource;
    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        mockDataSource = mock(HikariDataSource.class);
        mockConnection = mock(Connection.class);
        mockStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        companyRepository = new CompanyRepository(mockDataSource);
    }

    @Test
    void testFindByUserId() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getInt("user_id")).thenReturn(10);
        when(mockResultSet.getString("company_name")).thenReturn("TestCompany");
        when(mockResultSet.getString("website")).thenReturn("https://test.com");
        when(mockResultSet.getString("description")).thenReturn("Test description");
        when(mockResultSet.getTimestamp(anyString())).thenReturn(null);

        Optional<Company> result = companyRepository.findByUserId(10);

        assertTrue(result.isPresent());
        assertEquals("TestCompany", result.get().getCompanyName());
        verify(mockStatement).setInt(1, 10);
    }

    @Test
    void testFindByUserIdNotFound() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Optional<Company> result = companyRepository.findByUserId(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSave() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(42);

        Company company = new Company(10, "NewCompany");
        company.setWebsite("https://new.com");
        company.setDescription("New description");
        Company result = companyRepository.save(company);

        assertEquals(42, result.getId());
        assertEquals(10, result.getUserId());
        assertEquals("NewCompany", result.getCompanyName());
    }

    @Test
    void testUpdate() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        Company company = new Company(10, "UpdatedCompany");
        company.setId(1);
        company.setWebsite("https://updated.com");
        company.setDescription("Updated description");
        companyRepository.update(company);

        verify(mockStatement).setString(1, "UpdatedCompany");
        verify(mockStatement).setString(2, "https://updated.com");
        verify(mockStatement).setString(3, "Updated description");
        verify(mockStatement).setInt(5, 1);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testFindById() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getInt("user_id")).thenReturn(10);
        when(mockResultSet.getString("company_name")).thenReturn("TestCompany");
        when(mockResultSet.getString("website")).thenReturn("https://test.com");
        when(mockResultSet.getString("description")).thenReturn("Test description");
        when(mockResultSet.getTimestamp(anyString())).thenReturn(null);

        Optional<Company> result = companyRepository.findById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
        assertEquals("TestCompany", result.get().getCompanyName());
    }
}