package com.jobapp.server.service;

import com.jobapp.common.dto.TagDto;
import com.jobapp.server.model.Tag;
import com.jobapp.server.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagDto> getAllActiveTags() {
        logger.debug("Fetching all active tags");
        List<Tag> tags = tagRepository.findAll();
        List<TagDto> activeTags = tags.stream()
                .filter(tag -> Boolean.TRUE.equals(tag.getIsActive()))
                .map(this::toDto)
                .collect(Collectors.toList());
        logger.info("Found {} active tags", activeTags.size());
        return activeTags;
    }

    private TagDto toDto(Tag tag) {
        return new TagDto(tag.getId(), tag.getName(), tag.getIsActive());
    }
}