package com.jobapp.server.service;

import com.jobapp.common.dto.RecommendedVacancyDto;
import com.jobapp.common.dto.TagDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.model.VacancyWithTags;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000;

    private final VacancyRepository vacancyRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final CompanyRepository companyRepository;

    private final ConcurrentHashMap<Integer, CacheEntry> recommendationCache = new ConcurrentHashMap<>();

    public RecommendationService(VacancyRepository vacancyRepository,
                                UserRepository userRepository,
                                TagRepository tagRepository,
                                CompanyRepository companyRepository) {
        this.vacancyRepository = vacancyRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.companyRepository = companyRepository;
    }

    public List<RecommendedVacancyDto> getRecommendations(int userId) {
        CacheEntry cached = recommendationCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Returning cached recommendations for user: {}", userId);
            return cached.recommendations;
        }

        Set<Integer> userTagIds = userRepository.findUserTagIds(userId);
        if (userTagIds.isEmpty()) {
            logger.debug("User {} has no tags, returning empty list", userId);
            return Collections.emptyList();
        }

        List<VacancyWithTags> vacanciesWithTags = vacancyRepository.findAllActiveWithTags();

        List<RecommendedVacancyDto> recommendations = new ArrayList<>();

        for (VacancyWithTags vwt : vacanciesWithTags) {
            Set<Integer> vacancyTagIds = vwt.getTagIds();

            if (Collections.disjoint(userTagIds, vacancyTagIds)) {
                continue;
            }

            int intersection = countIntersection(userTagIds, vacancyTagIds);
            int union = userTagIds.size() + vacancyTagIds.size() - intersection;
            double matchPercent = (intersection * 2.0 / union) * 100;

            if (matchPercent > 0) {
                Optional<Vacancy> vacancyOpt = vacancyRepository.findById(vwt.getVacancyId());
                if (vacancyOpt.isPresent()) {
                    Vacancy vacancy = vacancyOpt.get();
                    RecommendedVacancyDto dto = toRecommendedVacancyDto(vacancy, matchPercent);
                    dto.setMatchPercent(matchPercent);
                    recommendations.add(dto);
                }
            }
        }

        recommendations.sort((a, b) -> Double.compare(
                b.getMatchPercent() != null ? b.getMatchPercent() : 0.0,
                a.getMatchPercent() != null ? a.getMatchPercent() : 0.0
        ));

        recommendationCache.put(userId, new CacheEntry(recommendations));
        logger.info("Generated {} recommendations for user: {}", recommendations.size(), userId);

        return recommendations;
    }

    public void clearCache(int userId) {
        recommendationCache.remove(userId);
        logger.debug("Cleared recommendation cache for user: {}", userId);
    }

    private int countIntersection(Set<Integer> set1, Set<Integer> set2) {
        int count = 0;
        for (Integer tagId : set1) {
            if (set2.contains(tagId)) {
                count++;
            }
        }
        return count;
    }

    private RecommendedVacancyDto toRecommendedVacancyDto(Vacancy vacancy, double matchPercent) {
        String companyName = null;
        if (vacancy.getCompanyId() != null) {
            Optional<Company> companyOpt = companyRepository.findById(vacancy.getCompanyId());
            companyName = companyOpt.map(Company::getCompanyName).orElse(null);
        }

        List<Tag> tags = tagRepository.findByVacancyId(vacancy.getId());
        List<TagDto> tagDtos = tags.stream()
                .map(t -> new TagDto(t.getId(), t.getName(), t.getIsActive()))
                .collect(Collectors.toList());

        return new RecommendedVacancyDto(
                vacancy.getId(),
                vacancy.getCompanyId(),
                companyName,
                vacancy.getTitle(),
                vacancy.getDescription(),
                vacancy.getCity(),
                vacancy.getSalaryMin(),
                vacancy.getSalaryMax(),
                vacancy.getEmploymentType(),
                vacancy.getIsActive(),
                vacancy.getCreatedAt(),
                tagDtos,
                matchPercent
        );
    }

    private class CacheEntry {
        private final List<RecommendedVacancyDto> recommendations;
        private final long timestamp;

        CacheEntry(List<RecommendedVacancyDto> recommendations) {
            this.recommendations = recommendations;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }
}