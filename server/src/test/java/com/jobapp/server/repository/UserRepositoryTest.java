package com.jobapp.server.repository;

import com.jobapp.server.model.Tag;
import com.jobapp.server.model.User;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserRepositoryTest {

    private UserRepository userRepository;
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

        userRepository = new UserRepository(mockDataSource);
    }

    @Test
    void testFindByEmail() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("email")).thenReturn("test@example.com");
        when(mockResultSet.getString("password_hash")).thenReturn("hash");
        when(mockResultSet.getString("role")).thenReturn("USER");
        when(mockResultSet.getString("name")).thenReturn("John");
        when(mockResultSet.getString("surname")).thenReturn("Doe");
        when(mockResultSet.getString("city")).thenReturn("Moscow");
        when(mockResultSet.getString("phone")).thenReturn("+1234567890");
        when(mockResultSet.getString("additional_info")).thenReturn("info");
        when(mockResultSet.getBoolean("is_blocked")).thenReturn(false);
        when(mockResultSet.getTimestamp(anyString())).thenReturn(null);

        Optional<User> result = userRepository.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(mockStatement).setString(1, "test@example.com");
    }

    @Test
    void testFindByEmailNotFound() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void testSave() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(42);

        User user = new User("new@example.com", "hash", "USER", "Jane", "Smith");
        User result = userRepository.save(user);

        assertEquals(42, result.getId());
        verify(mockStatement).setString(1, "new@example.com");
    }

    @Test
    void testUpdate() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        User user = new User("update@example.com", "newhash", "USER", "Updated", "Name");
        user.setId(1);
        userRepository.update(user);

        verify(mockStatement).executeUpdate();
    }

    @Test
    void testFindById() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("email")).thenReturn("test@example.com");
        when(mockResultSet.getString("password_hash")).thenReturn("hash");
        when(mockResultSet.getString("role")).thenReturn("USER");
        when(mockResultSet.getString("name")).thenReturn("John");
        when(mockResultSet.getString("surname")).thenReturn("Doe");
        when(mockResultSet.getBoolean("is_blocked")).thenReturn(false);
        when(mockResultSet.getTimestamp(anyString())).thenReturn(null);

        Optional<User> result = userRepository.findById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    void testDelete() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        userRepository.delete(1);

        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testExistsByEmail() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        boolean exists = userRepository.existsByEmail("exists@example.com");

        assertTrue(exists);
    }

    @Test
    void testExistsByEmailNotFound() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertFalse(exists);
    }

    @Test
    void testUpdatePassword() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        userRepository.updatePassword(1, "newPasswordHash");

        verify(mockStatement).setString(1, "newPasswordHash");
        verify(mockStatement).setInt(3, 1);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testUpdateProfile() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        userRepository.updateProfile(1, "John", "Doe", "Moscow", "+1234567890", "Some info");

        verify(mockStatement).setString(1, "John");
        verify(mockStatement).setString(2, "Doe");
        verify(mockStatement).setString(3, "Moscow");
        verify(mockStatement).setString(4, "+1234567890");
        verify(mockStatement).setString(5, "Some info");
        verify(mockStatement).setInt(7, 1);
        verify(mockStatement).executeUpdate();
    }

    @Test
    void testFindUserTags() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getString("name")).thenReturn("Java", "Python");
        when(mockResultSet.getBoolean("is_active")).thenReturn(true, true);
        when(mockResultSet.getTimestamp(anyString())).thenReturn(null);

        List<Tag> tags = userRepository.findUserTags(1);

        assertEquals(2, tags.size());
        assertEquals("Java", tags.get(0).getName());
        assertEquals("Python", tags.get(1).getName());
    }

    @Test
    void testUpdateUserTags() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);
        when(mockStatement.executeBatch()).thenReturn(new int[0]);
        doNothing().when(mockConnection).setAutoCommit(false);

        List<Integer> tagIds = new ArrayList<>();
        tagIds.add(1);
        tagIds.add(2);
        userRepository.updateUserTags(1, tagIds);

        verify(mockConnection).setAutoCommit(false);
        verify(mockStatement).executeBatch();
    }
}