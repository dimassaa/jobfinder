package com.jobapp.server.service;

import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.ViewHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(ViewHistoryService.class);

    private static final int DEFAULT_LIMIT = 20;

    private final ViewHistoryRepository viewHistoryRepository;
    private final CompanyRepository companyRepository;
    private final TagRepository tagRepository;

    public ViewHistoryService(ViewHistoryRepository viewHistoryRepository, CompanyRepository companyRepository, TagRepository tagRepository) {
        this.viewHistoryRepository = viewHistoryRepository;
        this.companyRepository = companyRepository;
        this.tagRepository = tagRepository;
    }

    public void addView(int userId, int vacancyId) {
        logger.debug("Recording view of vacancy {} by user {}", vacancyId, userId);
        viewHistoryRepository.add(userId, vacancyId);
    }

    public List<VacancyDto> getRecentViews(int userId) {
        return getRecentViews(userId, DEFAULT_LIMIT);
    }

    public List<VacancyDto> getRecentViews(int userId, int limit) {
        logger.debug("Getting recent views for user {} with limit {}", userId, limit);
        List<Vacancy> vacancies = viewHistoryRepository.getRecentViews(userId, limit);
        return vacancies.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private VacancyDto toDto(Vacancy vacancy) {
        Optional<Company> companyOpt = companyRepository.findById(vacancy.getCompanyId());
        Company company = companyOpt.orElse(null);

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