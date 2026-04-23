package com.jobapp.server.service;

import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.FavoriteRepository;
import com.jobapp.server.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FavoriteService {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteService.class);

    private final FavoriteRepository favoriteRepository;
    private final CompanyRepository companyRepository;
    private final TagRepository tagRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, CompanyRepository companyRepository, TagRepository tagRepository) {
        this.favoriteRepository = favoriteRepository;
        this.companyRepository = companyRepository;
        this.tagRepository = tagRepository;
    }

    public void add(int userId, int vacancyId) {
        logger.debug("Adding vacancy {} to favorites for user {}", vacancyId, userId);
        favoriteRepository.add(userId, vacancyId);
    }

    public void remove(int userId, int vacancyId) {
        logger.debug("Removing vacancy {} from favorites for user {}", vacancyId, userId);
        favoriteRepository.remove(userId, vacancyId);
    }

    public List<VacancyDto> getFavorites(int userId) {
        logger.debug("Getting favorites for user {}", userId);
        List<Vacancy> vacancies = favoriteRepository.findFavoritesByUser(userId);
        return vacancies.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public boolean isFavorite(int userId, int vacancyId) {
        return favoriteRepository.exists(userId, vacancyId);
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