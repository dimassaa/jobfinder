package com.jobapp.server.repository;

import com.jobapp.server.model.PasswordResetToken;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PasswordResetTokenRepositoryTest {

    private PasswordResetTokenRepository tokenRepository;
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

        tokenRepository = new PasswordResetTokenRepository(mockDataSource);
    }

    @Test
    void testSave() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        PasswordResetToken token = new PasswordResetToken(
            1,
            "test-token-123",
            LocalDateTime.now().plusHours(24)
        );

        tokenRepository.save(token);

        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).setString(2, "test-token-123");
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testFindByToken() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getInt("user_id")).thenReturn(1);
        when(mockResultSet.getString("token")).thenReturn("test-token-123");

        Optional<PasswordResetToken> result = tokenRepository.findByToken("test-token-123");

        assertTrue(result.isPresent());
        assertEquals("test-token-123", result.get().getToken());
        assertEquals(1, result.get().getUserId());
        verify(mockStatement).setString(1, "test-token-123");
    }

    @Test
    void testFindByTokenNotFound() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Optional<PasswordResetToken> result = tokenRepository.findByToken("nonexistent-token");

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteByUserId() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(2);

        tokenRepository.deleteByUserId(1);

        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testDeleteByToken() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        tokenRepository.deleteByToken("test-token-123");

        verify(mockStatement).setString(1, "test-token-123");
        verify(mockStatement).executeUpdate();
    }
}