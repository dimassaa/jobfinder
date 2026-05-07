package com.jobapp.server.service;

import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.FavoriteRepository;
import com.jobapp.server.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private TagRepository tagRepository;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, companyRepository, tagRepository);
    }

    @Test
    void add_shouldCallRepository() {
        favoriteService.add(1, 10);
        verify(favoriteRepository).add(1, 10);
    }

    @Test
    void remove_shouldCallRepository() {
        favoriteService.remove(1, 10);
        verify(favoriteRepository).remove(1, 10);
    }

    @Test
    void getFavorites_shouldReturnVacancyDtos() {
        Vacancy vacancy1 = new Vacancy();
        vacancy1.setId(10);
        vacancy1.setCompanyId(1);
        vacancy1.setTitle("Java Developer");
        vacancy1.setDescription("Backend developer");
        vacancy1.setCity("Минск");
        vacancy1.setSalaryMin(2000);
        vacancy1.setSalaryMax(4000);
        vacancy1.setEmploymentType("FULL_TIME");
        vacancy1.setIsActive(true);
        vacancy1.setCreatedAt(LocalDateTime.now());

        Vacancy vacancy2 = new Vacancy();
        vacancy2.setId(20);
        vacancy2.setCompanyId(2);
        vacancy2.setTitle("Python Developer");
        vacancy2.setDescription("Data scientist");
        vacancy2.setCity("Saint-Petersburg");
        vacancy2.setIsActive(true);

        Company company1 = new Company();
        company1.setId(1);
        company1.setCompanyName("TechCorp");

        Company company2 = new Company();
        company2.setId(2);
        company2.setCompanyName("DataInc");

        when(favoriteRepository.findFavoritesByUser(1)).thenReturn(Arrays.asList(vacancy1, vacancy2));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company1));
        when(companyRepository.findById(2)).thenReturn(Optional.of(company2));
        when(tagRepository.findByVacancyId(10)).thenReturn(Collections.emptyList());
        when(tagRepository.findByVacancyId(20)).thenReturn(Collections.emptyList());

        List<VacancyDto> result = favoriteService.getFavorites(1);

        assertEquals(2, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
        assertEquals("TechCorp", result.get(0).getCompanyName());
        assertEquals("Python Developer", result.get(1).getTitle());
        assertEquals("DataInc", result.get(1).getCompanyName());
    }

    @Test
    void getFavorites_shouldReturnEmptyListWhenNoFavorites() {
        when(favoriteRepository.findFavoritesByUser(1)).thenReturn(Collections.emptyList());

        List<VacancyDto> result = favoriteService.getFavorites(1);

        assertTrue(result.isEmpty());
    }

    @Test
    void isFavorite_shouldReturnTrueWhenExists() {
        when(favoriteRepository.exists(1, 10)).thenReturn(true);

        assertTrue(favoriteService.isFavorite(1, 10));
    }

    @Test
    void isFavorite_shouldReturnFalseWhenNotExists() {
        when(favoriteRepository.exists(1, 10)).thenReturn(false);

        assertFalse(favoriteService.isFavorite(1, 10));
    }
}