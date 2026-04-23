package com.jobapp.server.service;

import com.jobapp.common.dto.TagDto;
import com.jobapp.server.model.Tag;
import com.jobapp.server.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository);
    }

    @Test
    void getAllActiveTags_shouldReturnOnlyActiveTags() {
        Tag activeTag1 = new Tag("Java");
        activeTag1.setId(1);
        activeTag1.setIsActive(true);

        Tag activeTag2 = new Tag("Python");
        activeTag2.setId(2);
        activeTag2.setIsActive(true);

        Tag inactiveTag = new Tag("COBOL");
        inactiveTag.setId(3);
        inactiveTag.setIsActive(false);

        when(tagRepository.findAll()).thenReturn(Arrays.asList(activeTag1, activeTag2, inactiveTag));

        List<TagDto> result = tagService.getAllActiveTags();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("Java")));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("Python")));
        assertFalse(result.stream().anyMatch(t -> t.getName().equals("COBOL")));
    }

    @Test
    void getAllActiveTags_shouldReturnEmptyListWhenNoTags() {
        when(tagRepository.findAll()).thenReturn(Collections.emptyList());

        List<TagDto> result = tagService.getAllActiveTags();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllActiveTags_shouldReturnEmptyListWhenAllInactive() {
        Tag inactiveTag1 = new Tag("Tag1");
        inactiveTag1.setId(1);
        inactiveTag1.setIsActive(false);

        Tag inactiveTag2 = new Tag("Tag2");
        inactiveTag2.setId(2);
        inactiveTag2.setIsActive(false);

        when(tagRepository.findAll()).thenReturn(Arrays.asList(inactiveTag1, inactiveTag2));

        List<TagDto> result = tagService.getAllActiveTags();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllActiveTags_shouldConvertTagToDtoCorrectly() {
        Tag tag = new Tag("Spring");
        tag.setId(10);
        tag.setIsActive(true);

        when(tagRepository.findAll()).thenReturn(Collections.singletonList(tag));

        List<TagDto> result = tagService.getAllActiveTags();

        assertEquals(1, result.size());
        TagDto dto = result.get(0);
        assertEquals(10, dto.getId());
        assertEquals("Spring", dto.getName());
        assertTrue(dto.getIsActive());
    }

    @Test
    void getAllActiveTags_shouldCallRepositoryOnce() {
        when(tagRepository.findAll()).thenReturn(Collections.emptyList());

        tagService.getAllActiveTags();

        verify(tagRepository, times(1)).findAll();
    }
}