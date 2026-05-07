package com.jobapp.server.service;

import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.VacancyCreateRequest;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.common.dto.VacancyFilterDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.VacancyRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacancyServiceTest {

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private TagRepository tagRepository;

    private VacancyService vacancyService;

    @BeforeEach
    void setUp() {
        vacancyService = new VacancyService(vacancyRepository, companyRepository, tagRepository);
    }

    @Test
    void create_shouldCreateVacancyForOwner() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);
        company.setCompanyName("Tech Corp");

        VacancyCreateRequest req = new VacancyCreateRequest(
"Java Developer",
                "Description",
                "Минск",
                2000,
                4000,
                "FULL_TIME",
                Arrays.asList(1, 2)
        );

        Vacancy savedVacancy = new Vacancy();

        savedVacancy.setId(1);
        savedVacancy.setCompanyId(1);
        savedVacancy.setTitle("Java Developer");
        savedVacancy.setDescription("Description");
        savedVacancy.setCity("Минск");
        savedVacancy.setSalaryMin(2000);
        savedVacancy.setSalaryMax(4000);
        savedVacancy.setEmploymentType("FULL_TIME");
        savedVacancy.setIsActive(true);
        savedVacancy.setCreatedAt(LocalDateTime.now());

        Tag tag1 = new Tag("Java");
        tag1.setId(1);
        tag1.setIsActive(true);

        Tag tag2 = new Tag("SQL");
        tag2.setId(2);
        tag2.setIsActive(true);

        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(vacancyRepository.save(any(Vacancy.class))).thenReturn(savedVacancy);
        when(tagRepository.findById(1)).thenReturn(Optional.of(tag1));
        when(tagRepository.findById(2)).thenReturn(Optional.of(tag2));
        when(tagRepository.findByVacancyId(1)).thenReturn(Arrays.asList(tag1, tag2));

        VacancyDto result = vacancyService.create(1, req, 100);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Java Developer", result.getTitle());
        assertEquals("Tech Corp", result.getCompanyName());
        assertEquals(2, result.getTags().size());

        verify(vacancyRepository).addTags(eq(1), eq(Arrays.asList(1, 2)));
    }

    @Test
    void create_shouldThrowExceptionForNonOwner() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        VacancyCreateRequest req = new VacancyCreateRequest(
                "Java Developer", "Description", "Минск", 2000, 4000, "FULL_TIME", null
        );

        when(companyRepository.findById(1)).thenReturn(Optional.of(company));

        assertThrows(IllegalArgumentException.class, () ->
                vacancyService.create(1, req, 999)
        );
    }

    @Test
    void create_shouldThrowExceptionForNonExistentCompany() {
        VacancyCreateRequest req = new VacancyCreateRequest(
                "Java Developer", "Description", "Минск", 2000, 4000, "FULL_TIME", null
        );

        when(companyRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                vacancyService.create(999, req, 100)
        );
    }

    @Test
    void update_shouldUpdateVacancyForOwner() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);
        company.setCompanyName("Tech Corp");

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);
        vacancy.setTitle("Old Title");
        vacancy.setIsActive(true);

        VacancyCreateRequest req = new VacancyCreateRequest(
                "New Title", "New Description", "Минск", 2500, 4000, "PART_TIME", null
        );

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        doNothing().when(vacancyRepository).update(any(Vacancy.class));

        vacancyService.update(10, 100, req);

        verify(vacancyRepository).update(any(Vacancy.class));
    }

    @Test
    void update_shouldThrowExceptionForNonOwner() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);

        VacancyCreateRequest req = new VacancyCreateRequest(
                "New Title", "Description", "City", 2000, 4000, "FULL_TIME", null
        );

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));

        assertThrows(IllegalArgumentException.class, () ->
                vacancyService.update(10, 999, req)
        );
    }

    @Test
    void close_shouldCloseVacancyForOwner() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        doNothing().when(vacancyRepository).close(10);

        vacancyService.close(10, 100);

        verify(vacancyRepository).close(10);
    }

    @Test
    void close_shouldThrowExceptionForNonOwner() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));

        assertThrows(IllegalArgumentException.class, () ->
                vacancyService.close(10, 999)
        );
    }

    @Test
    void close_shouldThrowExceptionForNonExistentVacancy() {
        when(vacancyRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                vacancyService.close(999, 100)
        );
    }

    @Test
    void getById_shouldReturnVacancyDto() {
        Company company = new Company();
        company.setId(1);
        company.setCompanyName("Tech Corp");

        Vacancy vacancy = new Vacancy();
        vacancy.setId(10);
        vacancy.setCompanyId(1);
        vacancy.setTitle("Java Developer");
        vacancy.setDescription("Description");
        vacancy.setCity("Минск");
        vacancy.setSalaryMin(2000);
        vacancy.setSalaryMax(4000);
        vacancy.setEmploymentType("FULL_TIME");
        vacancy.setIsActive(true);
        vacancy.setCreatedAt(LocalDateTime.now());

        Tag tag = new Tag("Java");
        tag.setId(1);
        tag.setIsActive(true);

        when(vacancyRepository.findById(10)).thenReturn(Optional.of(vacancy));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(tagRepository.findByVacancyId(10)).thenReturn(Collections.singletonList(tag));

        VacancyDto result = vacancyService.getById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Java Developer", result.getTitle());
        assertEquals("Tech Corp", result.getCompanyName());
        assertEquals(1, result.getTags().size());
    }

    @Test
    void getById_shouldReturnNullForNonExistent() {
        when(vacancyRepository.findById(999)).thenReturn(Optional.empty());

        VacancyDto result = vacancyService.getById(999);

        assertNull(result);
    }

    @Test
    void search_shouldReturnListOfVacancies() {
        VacancyFilterDto filter = new VacancyFilterDto();
        filter.setCity("Минск");
        filter.setKeyword("Java");

        Vacancy vacancy1 = new Vacancy();
        vacancy1.setId(10);
        vacancy1.setCompanyId(1);
        vacancy1.setTitle("Java Developer");
        vacancy1.setDescription("Description");
        vacancy1.setIsActive(true);
        vacancy1.setCreatedAt(LocalDateTime.now());

        Vacancy vacancy2 = new Vacancy();
        vacancy2.setId(11);
        vacancy2.setCompanyId(1);
        vacancy2.setTitle("Java Senior");
        vacancy2.setDescription("Description");
        vacancy2.setIsActive(true);
        vacancy2.setCreatedAt(LocalDateTime.now());

        Company company = new Company();
        company.setId(1);
        company.setCompanyName("Tech Corp");

        when(vacancyRepository.search(filter)).thenReturn(Arrays.asList(vacancy1, vacancy2));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(tagRepository.findByVacancyId(anyInt())).thenReturn(Collections.emptyList());

        List<VacancyDto> result = vacancyService.search(filter);

        assertEquals(2, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
        assertEquals("Java Senior", result.get(1).getTitle());
    }

    @Test
    void getByCompany_shouldReturnVacanciesForCompany() {
        Vacancy vacancy1 = new Vacancy();
        vacancy1.setId(10);
        vacancy1.setCompanyId(1);
        vacancy1.setTitle("Java Developer");
        vacancy1.setDescription("Description");
        vacancy1.setIsActive(true);
        vacancy1.setCreatedAt(LocalDateTime.now());

        Company company = new Company();
        company.setId(1);
        company.setCompanyName("Tech Corp");

        when(vacancyRepository.findByCompanyId(1)).thenReturn(Collections.singletonList(vacancy1));
        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(tagRepository.findByVacancyId(10)).thenReturn(Collections.emptyList());

        List<VacancyDto> result = vacancyService.getByCompany(1);

        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
    }

    @Test
    void create_shouldValidateTagsOnCreate() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        VacancyCreateRequest req = new VacancyCreateRequest(
                "Java Developer", "Description", "Минск", 2000, 4000, "FULL_TIME",
                Arrays.asList(1, 999)
        );

        Vacancy savedVacancy = new Vacancy();
        savedVacancy.setId(10);
        savedVacancy.setCompanyId(1);
        savedVacancy.setIsActive(true);

        Tag tag1 = new Tag("Java");
        tag1.setId(1);
        tag1.setIsActive(true);

        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(vacancyRepository.save(any(Vacancy.class))).thenReturn(savedVacancy);
        when(tagRepository.findById(1)).thenReturn(Optional.of(tag1));
        when(tagRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                vacancyService.create(1, req, 100)
        );
    }

    @Test
    void create_shouldValidateInactiveTags() {
        Company company = new Company();
        company.setId(1);
        company.setUserId(100);

        VacancyCreateRequest req = new VacancyCreateRequest(
                "Java Developer", "Description", "Минск", 2000, 4000, "FULL_TIME",
                Arrays.asList(1)
        );

        Vacancy savedVacancy = new Vacancy();
        savedVacancy.setId(10);
        savedVacancy.setCompanyId(1);
        savedVacancy.setIsActive(true);

        Tag inactiveTag = new Tag("Inactive");
        inactiveTag.setId(1);
        inactiveTag.setIsActive(false);

        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(vacancyRepository.save(any(Vacancy.class))).thenReturn(savedVacancy);
        when(tagRepository.findById(1)).thenReturn(Optional.of(inactiveTag));

        assertThrows(IllegalArgumentException.class, () ->
                vacancyService.create(1, req, 100)
        );
    }
}