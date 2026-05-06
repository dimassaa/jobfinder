package com.jobapp.server.service;

import com.jobapp.common.dto.RecommendedVacancyDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.model.VacancyWithTags;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private CompanyRepository companyRepository;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                vacancyRepository,
                userRepository,
                tagRepository,
                companyRepository
        );
    }

    @Test
    void getRecommendations_shouldReturnEmptyListWhenUserHasNoTags() {
        when(userRepository.findUserTagIds(1)).thenReturn(Collections.emptySet());

        List<RecommendedVacancyDto> result = recommendationService.getRecommendations(1);

        assertTrue(result.isEmpty());
        verify(userRepository).findUserTagIds(1);
        verify(vacancyRepository, never()).findAllActiveWithTags();
    }

    @Test
    void getRecommendations_shouldCalculateMatchPercentCorrectly() {
        Set<Integer> userTagIds = new HashSet<>(Arrays.asList(1, 2));
        when(userRepository.findUserTagIds(1)).thenReturn(userTagIds);

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);
        vacancy.setTitle("Java Developer");
        vacancy.setDescription("Backend developer");
        vacancy.setCity("Minsk");
        vacancy.setIsActive(true);
        vacancy.setCreatedAt(LocalDateTime.now());

        VacancyWithTags vwt = new VacancyWithTags(10, new HashSet<>(Arrays.asList(1, 3)));

        Company company = new Company();
        company.setId(1);
        company.setCompanyName("TechCorp");

        Tag tag1 = new Tag();
        tag1.setId(1);
        tag1.setName("Java");
        tag1.setIsActive(true);

        Tag tag3 = new Tag();
        tag3.setId(3);
        tag3.setName("Docker");
        tag3.setIsActive(true);

        when(vacancyRepository.findAllActiveWithTags()).thenReturn(Collections.singletonList(vwt));
        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(tagRepository.findByVacancyId(10)).thenReturn(Arrays.asList(tag1, tag3));

        List<RecommendedVacancyDto> result = recommendationService.getRecommendations(1);

        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());

        double expectedIntersection = 1;
        double expectedUnion = 2 + 2 - 1;
        double expectedMatchPercent = (expectedIntersection * 2.0 / expectedUnion) * 100;
        assertEquals(expectedMatchPercent, result.get(0).getMatchPercent(), 0.01);
    }

    @Test
    void getRecommendations_shouldSortByMatchPercentDesc() {
        Set<Integer> userTagIds = new HashSet<>(Arrays.asList(1, 2));
        when(userRepository.findUserTagIds(1)).thenReturn(userTagIds);

        Vacancy vacancy1 = createVacancy(10, "Java Developer", 1);
        Vacancy vacancy2 = createVacancy(20, "Python Developer", 2);
        Vacancy vacancy3 = createVacancy(30, "Full Stack Developer", 3);

        VacancyWithTags vwt1 = new VacancyWithTags(10, new HashSet<>(Arrays.asList(1, 2)));
        VacancyWithTags vwt2 = new VacancyWithTags(20, new HashSet<>(Arrays.asList(2)));
        VacancyWithTags vwt3 = new VacancyWithTags(30, new HashSet<>(Arrays.asList(1)));

        Company company1 = createCompany(1, "TechCorp");
        Company company2 = createCompany(2, "DataInc");

        when(vacancyRepository.findAllActiveWithTags()).thenReturn(Arrays.asList(vwt1, vwt2, vwt3));
        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy1));
        when(vacancyRepository.findById(20)).thenReturn(Optional.of(vacancy2));
        when(vacancyRepository.findById(30)).thenReturn(Optional.of(vacancy3));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company1));
        when(companyRepository.findById(2)).thenReturn(Optional.of(company2));
        when(tagRepository.findByVacancyId(anyInt())).thenReturn(Collections.emptyList());

        List<RecommendedVacancyDto> result = recommendationService.getRecommendations(1);

        assertEquals(3, result.size());
        assertTrue(result.get(0).getMatchPercent() >= result.get(1).getMatchPercent());
        assertTrue(result.get(1).getMatchPercent() >= result.get(2).getMatchPercent());
    }

    @Test
    void getRecommendations_shouldExcludeVacanciesWithNoCommonTags() {
        Set<Integer> userTagIds = new HashSet<>(Arrays.asList(1, 2));
        when(userRepository.findUserTagIds(1)).thenReturn(userTagIds);

        Vacancy vacancy1 = createVacancy(10, "Java Developer", 1);
        Vacancy vacancy2 = createVacancy(20, "Python Developer", 2);

        VacancyWithTags vwt1 = new VacancyWithTags(10, new HashSet<>(Arrays.asList(1)));

        VacancyWithTags vwt2 = new VacancyWithTags(20, new HashSet<>(Arrays.asList(5, 6)));

        Company company1 = createCompany(1, "TechCorp");

        when(vacancyRepository.findAllActiveWithTags()).thenReturn(Arrays.asList(vwt1, vwt2));
        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy1));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company1));
        when(tagRepository.findByVacancyId(anyInt())).thenReturn(Collections.emptyList());

        List<RecommendedVacancyDto> result = recommendationService.getRecommendations(1);

        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
    }

    @Test
    void getRecommendations_shouldUseCacheOnSecondCall() {
        Set<Integer> userTagIds = new HashSet<>(Arrays.asList(1, 2));
        when(userRepository.findUserTagIds(1)).thenReturn(userTagIds);

        Vacancy vacancy = createVacancy(10, "Java Developer", 1);
        VacancyWithTags vwt = new VacancyWithTags(10, new HashSet<>(Arrays.asList(1)));
        Company company = createCompany(1, "TechCorp");

        when(vacancyRepository.findAllActiveWithTags()).thenReturn(Collections.singletonList(vwt));
        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(tagRepository.findByVacancyId(anyInt())).thenReturn(Collections.emptyList());

        List<RecommendedVacancyDto> result1 = recommendationService.getRecommendations(1);
        List<RecommendedVacancyDto> result2 = recommendationService.getRecommendations(1);

        assertEquals(result1.size(), result2.size());
        assertEquals("Java Developer", result2.get(0).getTitle());
        verify(vacancyRepository, times(1)).findAllActiveWithTags();
    }

    @Test
    void getRecommendations_shouldRecalculateAfterCacheClear() {
        Set<Integer> userTagIds = new HashSet<>(Arrays.asList(1, 2));
        when(userRepository.findUserTagIds(1)).thenReturn(userTagIds);

        Vacancy vacancy = createVacancy(10, "Java Developer", 1);
        VacancyWithTags vwt = new VacancyWithTags(10, new HashSet<>(Arrays.asList(1)));
        Company company = createCompany(1, "TechCorp");

        when(vacancyRepository.findAllActiveWithTags()).thenReturn(Collections.singletonList(vwt));
        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(tagRepository.findByVacancyId(anyInt())).thenReturn(Collections.emptyList());

        List<RecommendedVacancyDto> result1 = recommendationService.getRecommendations(1);

        recommendationService.clearCache(1);

        List<RecommendedVacancyDto> result2 = recommendationService.getRecommendations(1);

        assertEquals(result1.size(), result2.size());
        verify(vacancyRepository, times(2)).findAllActiveWithTags();
    }

    private Vacancy createVacancy(int id, String title, int companyId) {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(id);
        vacancy.setCompanyId(companyId);
        vacancy.setTitle(title);
        vacancy.setDescription("Description");
        vacancy.setCity("Minsk");
        vacancy.setIsActive(true);
        vacancy.setCreatedAt(LocalDateTime.now());
        return vacancy;
    }

    private Company createCompany(int id, String name) {
        Company company = new Company();
        company.setId(id);
        company.setCompanyName(name);
        return company;
    }
}