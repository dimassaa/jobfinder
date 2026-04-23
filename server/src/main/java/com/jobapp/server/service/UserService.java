package com.jobapp.server.service;

import com.jobapp.common.dto.ProfileDto;
import com.jobapp.common.dto.TagDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final TagRepository tagRepository;

    public UserService(UserRepository userRepository, CompanyRepository companyRepository, TagRepository tagRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.tagRepository = tagRepository;
    }

    public ProfileDto getProfile(int userId) {
        logger.debug("Getting profile for user: {}", userId);
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found: {}", userId);
            return null;
        }
        User user = userOpt.get();
        ProfileDto profileDto = new ProfileDto(
                user.getName(),
                user.getSurname(),
                user.getCity(),
                user.getPhone(),
                user.getAdditionalInfo()
        );

        if ("HR".equals(user.getRole())) {
            Optional<Company> companyOpt = companyRepository.findByUserId(userId);
            companyOpt.ifPresent(company -> {
                profileDto.setCompanyId(company.getId());
                profileDto.setCompanyName(company.getCompanyName());
                profileDto.setCompanyWebsite(company.getWebsite());
                profileDto.setCompanyDescription(company.getDescription());
            });
        }

        List<Tag> userTags = userRepository.findUserTags(userId);
        List<TagDto> tagDtos = userTags.stream()
                .map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getIsActive()))
                .collect(Collectors.toList());
        profileDto.setTags(tagDtos);

        logger.info("Profile retrieved for user: {}", userId);
        return profileDto;
    }

    public void updateProfile(int userId, ProfileDto dto) {
        logger.debug("Updating profile for user: {}", userId);
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found: {}", userId);
            return;
        }

        userRepository.updateProfile(userId, dto.getName(), dto.getSurname(), dto.getCity(), dto.getPhone(), dto.getAdditionalInfo());

        User user = userOpt.get();
        if ("HR".equals(user.getRole())) {
            Optional<Company> companyOpt = companyRepository.findByUserId(userId);
            if (companyOpt.isPresent()) {
                Company company = companyOpt.get();
                if (dto.getCompanyName() != null) {
                    company.setCompanyName(dto.getCompanyName());
                }
                company.setWebsite(dto.getCompanyWebsite());
                company.setDescription(dto.getCompanyDescription());
                companyRepository.update(company);
            } else if (dto.getCompanyName() != null) {
                Company company = new Company();
                company.setUserId(userId);
                company.setCompanyName(dto.getCompanyName());
                company.setWebsite(dto.getCompanyWebsite());
                company.setDescription(dto.getCompanyDescription());
                companyRepository.save(company);
            }
        }

        logger.info("Profile updated for user: {}", userId);
    }

    public List<TagDto> getUserTags(int userId) {
        logger.debug("Getting tags for user: {}", userId);
        List<Tag> tags = userRepository.findUserTags(userId);
        List<TagDto> tagDtos = tags.stream()
                .map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getIsActive()))
                .collect(Collectors.toList());
        logger.info("Found {} tags for user: {}", tagDtos.size(), userId);
        return tagDtos;
    }

    public void updateUserTags(int userId, List<Integer> tagIds) {
        logger.debug("Updating tags for user: {}", userId);

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

        userRepository.updateUserTags(userId, tagIds);
        logger.info("Tags updated for user: {}", userId);
    }
}