package com.jobapp.server.repository;

import com.jobapp.server.model.Application;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationRepositoryTest {

    private ApplicationRepository applicationRepository;
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

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        applicationRepository = new ApplicationRepository(mockDataSource);
    }

    @Test
    void testFindExistingFound() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id")).thenReturn(1);
            when(mockResultSet.getInt("vacancy_id")).thenReturn(10);
            when(mockResultSet.getInt("user_id")).thenReturn(100);
            when(mockResultSet.getString("status")).thenReturn("PENDING");
            return mockResultSet;
        });

        Optional<Application> result = applicationRepository.findExisting(100, 10);

        assertTrue(result.isPresent());
        assertEquals(10, result.get().getVacancyId());
        assertEquals(100, result.get().getUserId());
    }

    @Test
    void testFindExistingNotFound() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(false);
            return mockResultSet;
        });

        Optional<Application> result = applicationRepository.findExisting(100, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByVacancyIdWithUser() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id")).thenReturn(1);
            when(mockResultSet.getInt("vacancy_id")).thenReturn(10);
            when(mockResultSet.getInt("user_id")).thenReturn(100);
            when(mockResultSet.getString("status")).thenReturn("PENDING");
            when(mockResultSet.getString("user_name")).thenReturn("John");
            when(mockResultSet.getString("user_surname")).thenReturn("Doe");
            return mockResultSet;
        });

        List<Application> result = applicationRepository.findByVacancyIdWithUser(10);

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getUserName());
        assertEquals("Doe", result.get(0).getUserSurname());
    }

    @Test
    void testCountByUserLastMinute() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt(1)).thenReturn(5);
            return mockResultSet;
        });

        int count = applicationRepository.countByUserLastMinute(100);

        assertEquals(5, count);
    }

    @Test
    void testCountByUserLastMinuteZero() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt(1)).thenReturn(0);
            return mockResultSet;
        });

        int count = applicationRepository.countByUserLastMinute(100);

        assertEquals(0, count);
    }
}