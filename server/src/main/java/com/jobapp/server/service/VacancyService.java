package com.jobapp.server.service;

import com.jobapp.common.dto.NotificationDto;
import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.VacancyCreateRequest;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.common.dto.VacancyFilterDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.User;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class VacancyService {

    private static final Logger logger = LoggerFactory.getLogger(VacancyService.class);

    private final VacancyRepository vacancyRepository;
    private final CompanyRepository companyRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public VacancyService(VacancyRepository vacancyRepository,
                          CompanyRepository companyRepository,
                          TagRepository tagRepository) {
        this(vacancyRepository, companyRepository, tagRepository, null);
    }

    public VacancyService(VacancyRepository vacancyRepository,
                          CompanyRepository companyRepository,
                          TagRepository tagRepository,
                          NotificationService notificationService) {
        this(vacancyRepository, companyRepository, tagRepository, null, notificationService);
    }

    public VacancyService(VacancyRepository vacancyRepository,
                          CompanyRepository companyRepository,
                          TagRepository tagRepository,
                          UserRepository userRepository,
                          NotificationService notificationService) {
        this.vacancyRepository = vacancyRepository;
        this.companyRepository = companyRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository != null ? userRepository : new UserRepository();
        this.notificationService = notificationService;
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

        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (req.getDescription() == null || req.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (req.getSalaryMin() != null && req.getSalaryMax() != null && req.getSalaryMin() > req.getSalaryMax()) {
            throw new IllegalArgumentException("Salary minimum cannot be greater than maximum");
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

        checkAndNotifyMatchingUsers(vacancy.getId());

        return toDto(vacancy, company);
    }

    public void checkAndNotifyMatchingUsers(int vacancyId) {
        if (notificationService == null) {
            logger.warn("CRITICAL: NotificationService is null for vacancy {}. Users will not receive matching vacancy notifications.", vacancyId);
            return;
        }

        logger.info("Checking for matching users for vacancy {}", vacancyId);

        Optional<Vacancy> vacancyOpt = vacancyRepository.findById(vacancyId);
        if (vacancyOpt.isEmpty()) {
            logger.warn("Vacancy not found: {}", vacancyId);
            return;
        }

        Vacancy vacancy = vacancyOpt.get();
        Set<Integer> vacancyTagIds = vacancyRepository.getVacancyTagIds(vacancyId);

        if (vacancyTagIds.isEmpty()) {
            logger.debug("Vacancy {} has no tags, skipping notifications", vacancyId);
            return;
        }

        List<User> allUsers = userRepository.findAllUsersWithTags();
        Set<Integer> userIds = allUsers.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        Map<Integer, Set<Integer>> userTagIdsMap = userRepository.findUserTagIdsByUserIds(userIds);
        int notificationCount = 0;

        for (User user : allUsers) {
            if (user.getRole() == null || !"USER".equals(user.getRole())) {
                continue;
            }

            Set<Integer> userTagIds = userTagIdsMap.get(user.getId());
            if (userTagIds == null || userTagIds.isEmpty()) {
                continue;
            }

            Set<Integer> intersection = new java.util.HashSet<>(userTagIds);
            intersection.retainAll(vacancyTagIds);

            Set<Integer> union = new java.util.HashSet<>(userTagIds);
            union.addAll(vacancyTagIds);

            double matchPercent = (intersection.size() * 2.0 / union.size()) * 100;

            if (matchPercent >= 50.0) {
                NotificationDto notification = new NotificationDto();
                notification.setType("NEW_VACANCY");
                notification.setTitle("New Matching Vacancy");
                notification.setContent("A new vacancy matching your skills: " + vacancy.getTitle());
                notification.setLink("/vacancies/" + vacancyId);

                notificationService.sendToUser(user.getId(), notification);
                notificationCount++;

                logger.debug("Sent notification to user {} for vacancy {} (match: {}%)",
                        user.getId(), vacancyId, matchPercent);
            }
        }

        logger.info("Sent {} notifications for vacancy {}", notificationCount, vacancyId);
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
        Set<Integer> companyIds = vacancies.stream()
                .map(Vacancy::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> vacancyIds = vacancies.stream()
                .map(Vacancy::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Company> companyMap = companyRepository.findByIds(companyIds);
        Map<Integer, List<Tag>> tagsMap = tagRepository.findByVacancyIds(vacancyIds);
        return vacancies.stream()
                .map(v -> {
                    Company company = companyMap.get(v.getCompanyId());
                    List<Tag> tags = tagsMap.getOrDefault(v.getId(), Collections.emptyList());
                    return toDto(v, company, tags);
                })
                .collect(Collectors.toList());
    }

    public List<VacancyDto> getByCompany(int companyId) {
        logger.debug("Getting vacancies for company: {}", companyId);
        List<Vacancy> vacancies = vacancyRepository.findByCompanyId(companyId);
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        Company company = companyOpt.orElse(null);
        Set<Integer> vacancyIds = vacancies.stream()
                .map(Vacancy::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, List<Tag>> tagsMap = tagRepository.findByVacancyIds(vacancyIds);
        return vacancies.stream()
                .map(v -> {
                    List<Tag> tags = tagsMap.getOrDefault(v.getId(), Collections.emptyList());
                    return toDto(v, company, tags);
                })
                .collect(Collectors.toList());
    }

    private void validateTags(List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        Map<Integer, Tag> tagMap = tagRepository.findByIds(tagIds);
        for (Integer tagId : tagIds) {
            Tag tag = tagMap.get(tagId);
            if (tag == null) {
                logger.warn("Tag not found: {}", tagId);
                throw new IllegalArgumentException("Tag not found: " + tagId);
            }
            if (!Boolean.TRUE.equals(tag.getIsActive())) {
                logger.warn("Tag is not active: {}", tagId);
                throw new IllegalArgumentException("Tag is not active: " + tagId);
            }
        }
    }

    private VacancyDto toDto(Vacancy vacancy, Company company) {
        List<Tag> tags = tagRepository.findByVacancyId(vacancy.getId());
        return toDto(vacancy, company, tags);
    }

    private VacancyDto toDto(Vacancy vacancy, Company company, List<Tag> tags) {
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

        List<TagDto> tagDtos = tags.stream()
                .map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getIsActive()))
                .collect(Collectors.toList());
        dto.setTags(tagDtos);

        return dto;
    }
}