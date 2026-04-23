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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class VacancyService {

    private static final Logger logger = LoggerFactory.getLogger(VacancyService.class);

    private final VacancyRepository vacancyRepository;
    private final CompanyRepository companyRepository;
    private final TagRepository tagRepository;

    public VacancyService(VacancyRepository vacancyRepository, CompanyRepository companyRepository, TagRepository tagRepository) {
        this.vacancyRepository = vacancyRepository;
        this.companyRepository = companyRepository;
        this.tagRepository = tagRepository;
    }

    public VacancyDto create(int companyId, VacancyCreateRequest req, int userId) {
        logger.debug("Creating vacancy for company: {} by user: {}", companyId, userId);

        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            logger.warn("Company not found: {}", companyId);
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        Company company = companyOpt.get();
        if (company.getUserId() != userId) {
            logger.warn("User {} is not owner of company {}", userId, companyId);
            throw new IllegalArgumentException("User is not the owner of this company");
        }

        Vacancy vacancy = new Vacancy();
        vacancy.setCompanyId(companyId);
        vacancy.setTitle(req.getTitle());
        vacancy.setDescription(req.getDescription());
        vacancy.setCity(req.getCity());
        vacancy.setSalaryMin(req.getSalaryMin());
        vacancy.setSalaryMax(req.getSalaryMax());
        vacancy.setEmploymentType(req.getEmploymentType());
        vacancy.setIsActive(true);
        vacancy.setCreatedAt(LocalDateTime.now());

        vacancy = vacancyRepository.save(vacancy);

        if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
            validateTags(req.getTagIds());
            vacancyRepository.addTags(vacancy.getId(), req.getTagIds());
        }

        logger.info("Vacancy created with id: {}", vacancy.getId());
        return toDto(vacancy, company);
    }

    public void update(int vacancyId, int userId, VacancyCreateRequest req) {
        logger.debug("Updating vacancy: {} by user: {}", vacancyId, userId);

        Optional<Vacancy> vacancyOpt = vacancyRepository.findById(vacancyId);
        if (vacancyOpt.isEmpty()) {
            logger.warn("Vacancy not found: {}", vacancyId);
            throw new IllegalArgumentException("Vacancy not found: " + vacancyId);
        }

        Vacancy vacancy = vacancyOpt.get();

        Optional<Company> companyOpt = companyRepository.findById(vacancy.getCompanyId());
        if (companyOpt.isEmpty()) {
            logger.warn("Company not found for vacancy: {}", vacancyId);
            throw new IllegalArgumentException("Company not found");
        }

        Company company = companyOpt.get();
        if (company.getUserId() != userId) {
            logger.warn("User {} is not owner of company {}", userId, company.getId());
            throw new IllegalArgumentException("User is not the owner of this company");
        }

        vacancy.setTitle(req.getTitle());
        vacancy.setDescription(req.getDescription());
        vacancy.setCity(req.getCity());
        vacancy.setSalaryMin(req.getSalaryMin());
        vacancy.setSalaryMax(req.getSalaryMax());
        vacancy.setEmploymentType(req.getEmploymentType());

        vacancyRepository.update(vacancy);

        if (req.getTagIds() != null) {
            validateTags(req.getTagIds());
            vacancyRepository.clearTags(vacancyId);
            vacancyRepository.addTags(vacancyId, req.getTagIds());
        }

        logger.info("Vacancy updated: {}", vacancyId);
    }

    public void close(int vacancyId, int userId) {
        logger.debug("Closing vacancy: {} by user: {}", vacancyId, userId);

        Optional<Vacancy> vacancyOpt = vacancyRepository.findById(vacancyId);
        if (vacancyOpt.isEmpty()) {
            logger.warn("Vacancy not found: {}", vacancyId);
            throw new IllegalArgumentException("Vacancy not found: " + vacancyId);
        }

        Vacancy vacancy = vacancyOpt.get();

        Optional<Company> companyOpt = companyRepository.findById(vacancy.getCompanyId());
        if (companyOpt.isEmpty()) {
            logger.warn("Company not found for vacancy: {}", vacancyId);
            throw new IllegalArgumentException("Company not found");
        }

        Company company = companyOpt.get();
        if (company.getUserId() != userId) {
            logger.warn("User {} is not owner of company {}", userId, company.getId());
            throw new IllegalArgumentException("User is not the owner of this company");
        }

        vacancyRepository.close(vacancyId);
        logger.info("Vacancy closed: {}", vacancyId);
    }

    public VacancyDto getById(int vacancyId) {
        logger.debug("Getting vacancy by id: {}", vacancyId);

        Optional<Vacancy> vacancyOpt = vacancyRepository.findById(vacancyId);
        if (vacancyOpt.isEmpty()) {
            logger.warn("Vacancy not found: {}", vacancyId);
            return null;
        }

        Vacancy vacancy = vacancyOpt.get();
        Optional<Company> companyOpt = companyRepository.findById(vacancy.getCompanyId());
        Company company = companyOpt.orElse(null);

        return toDto(vacancy, company);
    }

    public List<VacancyDto> search(VacancyFilterDto filter) {
        logger.debug("Searching vacancies with filter");
        List<Vacancy> vacancies = vacancyRepository.search(filter);
        return vacancies.stream()
                .map(v -> {
                    Optional<Company> companyOpt = companyRepository.findById(v.getCompanyId());
                    Company company = companyOpt.orElse(null);
                    return toDto(v, company);
                })
                .collect(Collectors.toList());
    }

    public List<VacancyDto> getByCompany(int companyId) {
        logger.debug("Getting vacancies for company: {}", companyId);
        List<Vacancy> vacancies = vacancyRepository.findByCompanyId(companyId);
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        Company company = companyOpt.orElse(null);
        return vacancies.stream()
                .map(v -> toDto(v, company))
                .collect(Collectors.toList());
    }

    private void validateTags(List<Integer> tagIds) {
        for (Integer tagId : tagIds) {
            Optional<Tag> tagOpt = tagRepository.findById(tagId);
            if (tagOpt.isEmpty()) {
                logger.warn("Tag not found: {}", tagId);
                throw new IllegalArgumentException("Tag not found: " + tagId);
            }
            Tag tag = tagOpt.get();
            if (!Boolean.TRUE.equals(tag.getIsActive())) {
                logger.warn("Tag is not active: {}", tagId);
                throw new IllegalArgumentException("Tag is not active: " + tagId);
            }
        }
    }

    private VacancyDto toDto(Vacancy vacancy, Company company) {
        VacancyDto dto = new VacancyDto(
                vacancy.getId(),
                vacancy.getCompanyId(),
                company != null ? company.getCompanyName() : null,
                vacancy.getTitle(),
                vacancy.getDescription(),
                vacancy.getCity(),
                vacancy.getSalaryMin(),
                vacancy.getSalaryMax(),
                vacancy.getEmploymentType(),
                vacancy.getIsActive(),
                vacancy.getCreatedAt(),
                new ArrayList<>()
        );

        List<Tag> tags = tagRepository.findByVacancyId(vacancy.getId());
        List<TagDto> tagDtos = tags.stream()
                .map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getIsActive()))
                .collect(Collectors.toList());
        dto.setTags(tagDtos);

        return dto;
    }
}