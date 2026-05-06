package com.jobapp.server.repository;

import com.jobapp.common.dto.VacancyFilterDto;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.model.VacancyWithTags;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class VacancyRepositoryTest {

    private VacancyRepository vacancyRepository;
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

        vacancyRepository = new VacancyRepository(mockDataSource);
    }

    @Test
    void testSave() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("id")).thenReturn(1);
            return mockResultSet;
        });

        Vacancy vacancy = new Vacancy(1, "Senior Developer", "Description");
        vacancy.setCity("Minsk");
        vacancy.setSalaryMin(2000);
        vacancy.setSalaryMax(4000);
        vacancy.setEmploymentType("FULL_TIME");

        Vacancy result = vacancyRepository.save(vacancy);

        assertEquals(1, result.getId());
        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).setString(2, "Senior Developer");
    }

    @Test
    void testFindById() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id")).thenReturn(1);
            when(mockResultSet.getInt("company_id")).thenReturn(1);
            when(mockResultSet.getString("title")).thenReturn("Developer");
            when(mockResultSet.getString("description")).thenReturn("Test description");
            when(mockResultSet.getString("city")).thenReturn("Minsk");
            when(mockResultSet.getInt("salary_min")).thenReturn(2000);
            when(mockResultSet.wasNull()).thenReturn(false);
            when(mockResultSet.getInt("salary_max")).thenReturn(4000);
            when(mockResultSet.getString("employment_type")).thenReturn("FULL_TIME");
            when(mockResultSet.getBoolean("is_active")).thenReturn(true);
            when(mockResultSet.getTimestamp("created_at")).thenReturn(null);
            when(mockResultSet.getTimestamp("closed_at")).thenReturn(null);
            return mockResultSet;
        });

        var result = vacancyRepository.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Developer", result.get().getTitle());
        verify(mockStatement).setInt(1, 1);
    }

    @Test
    void testFindByIdNotFound() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(false);
            return mockResultSet;
        });

        var result = vacancyRepository.findById(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAllActive() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getInt("id")).thenReturn(1).thenReturn(2);
            when(mockResultSet.getInt("company_id")).thenReturn(1);
            when(mockResultSet.getString("title")).thenReturn("Developer 1").thenReturn("Developer 2");
            when(mockResultSet.getString("description")).thenReturn("Test description");
            when(mockResultSet.getString("city")).thenReturn("Minsk");
            when(mockResultSet.getInt("salary_min")).thenReturn(2000);
            when(mockResultSet.wasNull()).thenReturn(false);
            when(mockResultSet.getInt("salary_max")).thenReturn(4000);
            when(mockResultSet.getString("employment_type")).thenReturn("FULL_TIME");
            when(mockResultSet.getBoolean("is_active")).thenReturn(true);
            when(mockResultSet.getTimestamp("created_at")).thenReturn(null);
            when(mockResultSet.getTimestamp("closed_at")).thenReturn(null);
            return mockResultSet;
        });

        List<Vacancy> result = vacancyRepository.findAllActive();

        assertEquals(2, result.size());
        assertEquals("Developer 1", result.get(0).getTitle());
        assertEquals("Developer 2", result.get(1).getTitle());
    }

    @Test
    void testUpdate() throws SQLException {
        when(mockStatement.executeUpdate()).thenReturn(1);

        Vacancy vacancy = new Vacancy(1, "Updated Title", "Updated description");
        vacancy.setId(1);
        vacancy.setCity("St. Petersburg");
        vacancy.setIsActive(true);

        vacancyRepository.update(vacancy);

        verify(mockStatement).executeUpdate();
        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).setString(2, "Updated Title");
    }

    @Test
    void testClose() throws SQLException {
        when(mockStatement.executeUpdate()).thenReturn(1);

        vacancyRepository.close(1);

        verify(mockStatement).executeUpdate();
        verify(mockStatement).setInt(2, 1);
    }

    @Test
    void testSearchWithKeyword() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            setupVacancyRowMock(mockResultSet, 1, "Java Developer");
            return mockResultSet;
        });

        VacancyFilterDto filter = new VacancyFilterDto();
        filter.setKeyword("Java");

        List<Vacancy> result = vacancyRepository.search(filter);

        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
        verify(mockStatement).setString(1, "%Java%");
        verify(mockStatement).setString(2, "%Java%");
    }

    @Test
    void testSearchWithCity() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            setupVacancyRowMock(mockResultSet, 1, "Developer");
            return mockResultSet;
        });

        VacancyFilterDto filter = new VacancyFilterDto();
        filter.setCity("Minsk");

        List<Vacancy> result = vacancyRepository.search(filter);

        assertEquals(1, result.size());
        verify(mockStatement).setString(1, "Minsk");
    }

    @Test
    void testSearchWithSalaryRange() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            setupVacancyRowMock(mockResultSet, 1, "Developer");
            return mockResultSet;
        });

        VacancyFilterDto filter = new VacancyFilterDto();
        filter.setSalaryFrom(1000);
        filter.setSalaryTo(3000);

        List<Vacancy> result = vacancyRepository.search(filter);

        assertEquals(1, result.size());
        verify(mockStatement).setInt(1, 1000);
        verify(mockStatement).setInt(2, 3000);
    }

    @Test
    void testSearchWithEmploymentType() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            setupVacancyRowMock(mockResultSet, 1, "Remote Developer");
            return mockResultSet;
        });

        VacancyFilterDto filter = new VacancyFilterDto();
        filter.setEmploymentType("REMOTE");

        List<Vacancy> result = vacancyRepository.search(filter);

        assertEquals(1, result.size());
        verify(mockStatement).setString(1, "REMOTE");
    }

    @Test
    void testSearchWithTags() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            setupVacancyRowMock(mockResultSet, 1, "Java Developer");
            return mockResultSet;
        });

        VacancyFilterDto filter = new VacancyFilterDto();
        filter.setTagIds(Arrays.asList(1, 2, 3));

        List<Vacancy> result = vacancyRepository.search(filter);

        assertEquals(1, result.size());
        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).setInt(2, 2);
        verify(mockStatement).setInt(3, 3);
    }

    @Test
    void testSearchWithMultipleFilters() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            setupVacancyRowMock(mockResultSet, 1, "Java Developer in Minsk");
            return mockResultSet;
        });

        VacancyFilterDto filter = new VacancyFilterDto();
        filter.setKeyword("Java");
        filter.setCity("Minsk");
        filter.setSalaryFrom(2500);
        filter.setEmploymentType("FULL_TIME");

        List<Vacancy> result = vacancyRepository.search(filter);

        assertEquals(1, result.size());
    }

    @Test
    void testSearchEmptyResult() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(false);
            return mockResultSet;
        });

        VacancyFilterDto filter = new VacancyFilterDto();
        filter.setKeyword("NonExistent");

        List<Vacancy> result = vacancyRepository.search(filter);

        assertTrue(result.isEmpty());
    }

    @Test
    void testAddTags() throws SQLException {
        when(mockStatement.executeBatch()).thenReturn(new int[]{1, 1, 1});

        vacancyRepository.addTags(1, Arrays.asList(1, 2, 3));

        verify(mockStatement, times(3)).setInt(eq(1), eq(1));
        verify(mockStatement, times(3)).setInt(eq(2), anyInt());
        verify(mockStatement).executeBatch();
    }

    @Test
    void testAddTagsEmptyList() throws SQLException {
        vacancyRepository.addTags(1, new ArrayList<>());

        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    void testClearTags() throws SQLException {
        when(mockStatement.executeUpdate()).thenReturn(3);

        vacancyRepository.clearTags(1);

        verify(mockStatement).executeUpdate();
        verify(mockStatement).setInt(1, 1);
    }

    @Test
    void testFindByCompanyId() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            setupVacancyRowMock(mockResultSet, 1, "Company Vacancy");
            return mockResultSet;
        });

        List<Vacancy> result = vacancyRepository.findByCompanyId(1);

        assertEquals(1, result.size());
        assertEquals("Company Vacancy", result.get(0).getTitle());
        verify(mockStatement).setInt(1, 1);
    }

    private void setupVacancyRowMock(ResultSet rs, int id, String title) throws SQLException {
        when(rs.getInt("id")).thenReturn(id);
        when(rs.getInt("company_id")).thenReturn(1);
        when(rs.getString("title")).thenReturn(title);
        when(rs.getString("description")).thenReturn("Test description");
        when(rs.getString("city")).thenReturn("Minsk");
        when(rs.getInt("salary_min")).thenReturn(2000);
        when(rs.wasNull()).thenReturn(false);
        when(rs.getInt("salary_max")).thenReturn(4000);
        when(rs.getString("employment_type")).thenReturn("FULL_TIME");
        when(rs.getBoolean("is_active")).thenReturn(true);
        when(rs.getTimestamp("created_at")).thenReturn(null);
        when(rs.getTimestamp("closed_at")).thenReturn(null);
    }

    @Test
    void testFindAllActiveWithTags() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, true, true, false);
            when(mockResultSet.getInt("id")).thenReturn(1, 1, 2);
            when(mockResultSet.getInt("tag_id")).thenReturn(1, 2, 1);
            when(mockResultSet.wasNull()).thenReturn(false);
            return mockResultSet;
        });

        List<VacancyWithTags> result = vacancyRepository.findAllActiveWithTags();

        assertEquals(2, result.size());
        VacancyWithTags v1 = result.stream().filter(v -> v.getVacancyId() == 1).findFirst().orElseThrow();
        VacancyWithTags v2 = result.stream().filter(v -> v.getVacancyId() == 2).findFirst().orElseThrow();
        assertEquals(2, v1.getTagIds().size());
        assertTrue(v1.getTagIds().contains(1));
        assertTrue(v1.getTagIds().contains(2));
        assertEquals(1, v2.getTagIds().size());
        assertTrue(v2.getTagIds().contains(1));
    }

    @Test
    void testFindAllActiveWithTagsWithoutTags() throws SQLException {
        when(mockStatement.executeQuery()).thenAnswer(invocation -> {
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("id")).thenReturn(1);
            when(mockResultSet.getInt("tag_id")).thenReturn(0);
            when(mockResultSet.wasNull()).thenReturn(true);
            return mockResultSet;
        });

        List<VacancyWithTags> result = vacancyRepository.findAllActiveWithTags();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getTagIds().isEmpty());
    }
}