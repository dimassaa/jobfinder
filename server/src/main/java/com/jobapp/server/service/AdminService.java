package com.jobapp.server.service;

import com.jobapp.common.dto.AdminUserDto;
import com.jobapp.common.dto.ComplaintAdminDto;
import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.server.model.User;
import com.jobapp.server.model.Vacancy;
import com.jobapp.server.repository.BlockHistoryRepository;
import com.jobapp.server.repository.ComplaintRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import com.jobapp.server.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final VacancyRepository vacancyRepository;
    private final ComplaintRepository complaintRepository;
    private final TagRepository tagRepository;
    private final BlockHistoryRepository blockHistoryRepository;

    public AdminService() {
        this.userRepository = new UserRepository();
        this.vacancyRepository = new VacancyRepository();
        this.complaintRepository = new ComplaintRepository();
        this.tagRepository = new TagRepository();
        this.blockHistoryRepository = new BlockHistoryRepository();
    }

    public AdminService(UserRepository userRepository, VacancyRepository vacancyRepository,
                       ComplaintRepository complaintRepository, TagRepository tagRepository,
                       BlockHistoryRepository blockHistoryRepository) {
        this.userRepository = userRepository;
        this.vacancyRepository = vacancyRepository;
        this.complaintRepository = complaintRepository;
        this.tagRepository = tagRepository;
        this.blockHistoryRepository = blockHistoryRepository;
    }

    public List<AdminUserDto> getAllUsers(String roleFilter, Boolean blockFilter, int page, int size) {
        int offset = page * size;
        List<User> users = userRepository.findAllWithFilters(roleFilter, blockFilter, offset, size);
        List<AdminUserDto> dtos = new ArrayList<>();
        for (User user : users) {
            dtos.add(new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getName(),
                user.getSurname(),
                user.getCity(),
                user.getIsBlocked(),
                user.getBlockedUntil(),
                user.getCreatedAt()
            ));
        }
        logger.info("Retrieved {} users for roleFilter={}, blockFilter={}", dtos.size(), roleFilter, blockFilter);
        return dtos;
    }

    public void blockUser(int userId, int adminId, String reason, Integer durationHours) {
        LocalDateTime blockedUntil = null;
        if (durationHours != null && durationHours > 0) {
            blockedUntil = LocalDateTime.now().plusHours(durationHours);
        }
        userRepository.blockUser(userId, reason, blockedUntil, adminId);

        if (SessionManager.getInstance().isOnline(userId)) {
            SessionManager.getInstance().removeSession(userId);
            logger.info("Closed session for blocked user: {}", userId);
        }
        logger.info("User {} blocked by admin {} for {} hours", userId, adminId, durationHours);
    }

    public void unblockUser(int userId) {
        userRepository.unblockUser(userId);
        logger.info("User {} unblocked", userId);
    }

    public void deleteUser(int userId) {
        userRepository.softDelete(userId);

        if (SessionManager.getInstance().isOnline(userId)) {
            SessionManager.getInstance().removeSession(userId);
        }
        logger.info("User {} deleted", userId);
    }

    public List<VacancyDto> getAllVacancies(boolean includeInactive) {
        List<Vacancy> vacancies = vacancyRepository.findAllForAdmin(includeInactive);
        List<VacancyDto> dtos = new ArrayList<>();
        for (Vacancy vacancy : vacancies) {
            dtos.add(new VacancyDto(
                vacancy.getId(),
                vacancy.getCompanyId(),
                null,
                vacancy.getTitle(),
                vacancy.getDescription(),
                vacancy.getCity(),
                vacancy.getSalaryMin(),
                vacancy.getSalaryMax(),
                vacancy.getEmploymentType(),
                vacancy.getIsActive(),
                vacancy.getCreatedAt(),
                null
            ));
        }
        logger.info("Retrieved {} vacancies for admin", dtos.size());
        return dtos;
    }

    public void deleteVacancy(int vacancyId) {
        vacancyRepository.softDeleteByAdmin(vacancyId);
        logger.info("Vacancy {} deleted by admin", vacancyId);
    }

    public List<ComplaintAdminDto> getComplaints() {
        List<com.jobapp.server.model.Complaint> complaints = complaintRepository.findAllWithDetails();
        List<ComplaintAdminDto> dtos = new ArrayList<>();
        for (com.jobapp.server.model.Complaint complaint : complaints) {
            String targetName = null;
            if ("VACANCY".equals(complaint.getTargetType())) {
                var vacancy = vacancyRepository.findById(complaint.getTargetId());
                targetName = vacancy.map(v -> v.getTitle()).orElse(null);
            } else if ("USER".equals(complaint.getTargetType())) {
                var user = userRepository.findById(complaint.getTargetId());
                targetName = user.map(u -> u.getEmail()).orElse(null);
            }
            dtos.add(new ComplaintAdminDto(
                complaint.getId(),
                complaint.getUserId(),
                complaint.getTargetType(),
                complaint.getTargetId(),
                complaint.getReason(),
                complaint.getStatus(),
                complaint.getCreatedAt(),
                complaint.getResolvedBy(),
                complaint.getResolvedAt(),
                targetName
            ));
        }
        logger.info("Retrieved {} complaints for admin", dtos.size());
        return dtos;
    }

    public void resolveComplaint(int complaintId, String action, int adminId) {
        com.jobapp.server.model.Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint == null) {
            throw new RuntimeException("Complaint not found: " + complaintId);
        }

        complaintRepository.resolve(complaintId, action, adminId);

        if ("BLOCK_VACANCY".equals(action) && "VACANCY".equals(complaint.getTargetType())) {
            vacancyRepository.softDeleteByAdmin(complaint.getTargetId());
            logger.info("Vacancy {} blocked due to complaint", complaint.getTargetId());
        } else if ("BLOCK_USER".equals(action) && "USER".equals(complaint.getTargetType())) {
            userRepository.blockUser(complaint.getTargetId(), "Blocked due to complaint", null, adminId);
            if (SessionManager.getInstance().isOnline(complaint.getTargetId())) {
                SessionManager.getInstance().removeSession(complaint.getTargetId());
            }
            logger.info("User {} blocked due to complaint", complaint.getTargetId());
        }
        logger.info("Complaint {} resolved with action: {}", complaintId, action);
    }

    public void addTag(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }
        var existing = tagRepository.findByName(name.trim());
        if (existing.isPresent()) {
            throw new RuntimeException("Tag already exists: " + name);
        }
        var tag = new com.jobapp.server.model.Tag();
        tag.setName(name.trim());
        tag.setIsActive(true);
        tagRepository.save(tag);
        logger.info("Tag {} added", name);
    }

    public void deactivateTag(int tagId) {
        tagRepository.updateActiveStatus(tagId, false);
        logger.info("Tag {} deactivated", tagId);
    }

    public void createAdmin(String email, String password, String name) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }

        User admin = new User();
        admin.setEmail(email.trim());
        admin.setPasswordHash(PasswordUtil.hash(password));
        admin.setRole("ADMIN");
        admin.setName(name);
        admin.setSurname(null);
        admin.setIsBlocked(false);

        userRepository.save(admin);
        logger.info("Admin created: {}", email);
    }
}