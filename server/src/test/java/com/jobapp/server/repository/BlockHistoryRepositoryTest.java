package com.jobapp.server.repository;

import com.jobapp.server.model.BlockHistory;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BlockHistoryRepositoryTest {

    private BlockHistoryRepository blockHistoryRepository;
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

        blockHistoryRepository = new BlockHistoryRepository(mockDataSource);
    }

    @Test
    void testSave() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        BlockHistory entry = new BlockHistory(
            1,
            2,
            "Spam content",
            LocalDateTime.now().plusDays(7)
        );

        blockHistoryRepository.save(entry);

        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).setInt(2, 2);
        verify(mockStatement).setString(3, "Spam content");
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testSaveWithoutReason() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        BlockHistory entry = new BlockHistory(
            1,
            2,
            null,
            LocalDateTime.now().plusHours(24)
        );

        blockHistoryRepository.save(entry);

        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).setInt(2, 2);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testSavePermanentBlock() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        BlockHistory entry = new BlockHistory(
            1,
            2,
            "Repeated violations",
            null
        );

        blockHistoryRepository.save(entry);

        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).setInt(2, 2);
        verify(mockStatement).setString(3, "Repeated violations");
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testFindByUserId() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getInt("user_id")).thenReturn(1, 1);
        when(mockResultSet.getInt("blocked_by")).thenReturn(3, 3);
        when(mockResultSet.getString("reason")).thenReturn("Spam", "Abuse");

        List<BlockHistory> entries = blockHistoryRepository.findByUserId(1);

        assertEquals(2, entries.size());
        assertEquals("Spam", entries.get(0).getReason());
        assertEquals("Abuse", entries.get(1).getReason());
        verify(mockStatement).setInt(1, 1);
    }

    @Test
    void testFindByUserIdEmpty() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<BlockHistory> entries = blockHistoryRepository.findByUserId(999);

        assertTrue(entries.isEmpty());
    }
}