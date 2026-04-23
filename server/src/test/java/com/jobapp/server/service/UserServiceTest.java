package com.jobapp.server.service;

import com.jobapp.common.dto.ProfileDto;
import com.jobapp.common.dto.TagDto;
import com.jobapp.server.model.Company;
import com.jobapp.server.model.Tag;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private TagRepository tagRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, companyRepository, tagRepository);
    }

    @Test
    void getProfile_shouldReturnProfileForJobSeeker() {
        User user = new User();
        user.setId(1);
        user.setEmail("test@test.com");
        user.setRole("JOBSEEKER");
        user.setName("John");
        user.setSurname("Doe");
        user.setCity("Moscow");
        user.setPhone("+79991234567");
        user.setAdditionalInfo("Experienced developer");

        Tag tag1 = new Tag("Java");
        tag1.setId(1);
        tag1.setIsActive(true);

        Tag tag2 = new Tag("SQL");
        tag2.setId(2);
        tag2.setIsActive(true);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.findUserTags(1)).thenReturn(Arrays.asList(tag1, tag2));

        ProfileDto result = userService.getProfile(1);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals("Doe", result.getSurname());
        assertEquals("Moscow", result.getCity());
        assertEquals("+79991234567", result.getPhone());
        assertEquals("Experienced developer", result.getAdditionalInfo());
        assertEquals(2, result.getTags().size());
    }

    @Test
    void getProfile_shouldReturnProfileWithCompanyForHr() {
        User user = new User();
        user.setId(1);
        user.setEmail("hr@test.com");
        user.setRole("HR");
        user.setName("Jane");
        user.setSurname("Smith");

        Company company = new Company();
        company.setId(10);
        company.setCompanyName("Tech Corp");
        company.setWebsite("https://techcorp.com");
        company.setDescription("IT Company");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(1)).thenReturn(Optional.of(company));
        when(userRepository.findUserTags(1)).thenReturn(Collections.emptyList());

        ProfileDto result = userService.getProfile(1);

        assertNotNull(result);
        assertEquals("Jane", result.getName());
        assertEquals(10, result.getCompanyId());
        assertEquals("Tech Corp", result.getCompanyName());
        assertEquals("https://techcorp.com", result.getCompanyWebsite());
        assertEquals("IT Company", result.getCompanyDescription());
    }

    @Test
    void getProfile_shouldReturnNullWhenUserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        ProfileDto result = userService.getProfile(999);

        assertNull(result);
    }

    @Test
    void updateProfile_shouldUpdateUserFields() {
        User user = new User();
        user.setId(1);
        user.setRole("JOBSEEKER");

        ProfileDto dto = new ProfileDto("NewName", "NewSurname", "Spb", "+79991234567", "Info");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).updateProfile(eq(1), any(), any(), any(), any(), any());

        userService.updateProfile(1, dto);

        verify(userRepository).updateProfile(eq(1), eq("NewName"), eq("NewSurname"), eq("Spb"), eq("+79991234567"), eq("Info"));
    }

    @Test
    void updateProfile_shouldUpdateCompanyForHr() {
        User user = new User();
        user.setId(1);
        user.setRole("HR");

        Company company = new Company();
        company.setId(10);
        company.setCompanyName("Old Name");
        company.setUserId(1);

        ProfileDto dto = new ProfileDto("Name", "Surname", "City", "Phone", null);
        dto.setCompanyName("New Company");
        dto.setCompanyWebsite("https://newcompany.com");
        dto.setCompanyDescription("Description");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(1)).thenReturn(Optional.of(company));
        doNothing().when(userRepository).updateProfile(anyInt(), any(), any(), any(), any(), any());
        doNothing().when(companyRepository).update(any(Company.class));

        userService.updateProfile(1, dto);

        verify(companyRepository).update(any(Company.class));
    }

    @Test
    void updateProfile_shouldCreateCompanyIfNotExistsForHr() {
        User user = new User();
        user.setId(1);
        user.setRole("HR");

        ProfileDto dto = new ProfileDto("Name", "Surname", "City", "Phone", null);
        dto.setCompanyName("New Company");
        dto.setCompanyWebsite("https://newcompany.com");
        dto.setCompanyDescription("Description");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(companyRepository.findByUserId(1)).thenReturn(Optional.empty());
        doNothing().when(userRepository).updateProfile(anyInt(), any(), any(), any(), any(), any());
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateProfile(1, dto);

        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void getUserTags_shouldReturnTagsForUser() {
        Tag tag1 = new Tag("Java");
        tag1.setId(1);
        tag1.setIsActive(true);

        Tag tag2 = new Tag("Python");
        tag2.setId(2);
        tag2.setIsActive(true);

        when(userRepository.findUserTags(1)).thenReturn(Arrays.asList(tag1, tag2));

        List<TagDto> result = userService.getUserTags(1);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("Java")));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("Python")));
    }

    @Test
    void updateUserTags_shouldValidateAndUpdateTags() {
        Tag tag1 = new Tag("Java");
        tag1.setId(1);
        tag1.setIsActive(true);

        Tag tag2 = new Tag("SQL");
        tag2.setId(2);
        tag2.setIsActive(true);

        when(tagRepository.findById(1)).thenReturn(Optional.of(tag1));
        when(tagRepository.findById(2)).thenReturn(Optional.of(tag2));
        doNothing().when(userRepository).updateUserTags(eq(1), anyList());

        userService.updateUserTags(1, Arrays.asList(1, 2));

        verify(userRepository).updateUserTags(eq(1), eq(Arrays.asList(1, 2)));
    }

    @Test
    void updateUserTags_shouldThrowExceptionForInvalidTag() {
        when(tagRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            userService.updateUserTags(1, Arrays.asList(999))
        );
    }

    @Test
    void updateUserTags_shouldThrowExceptionForInactiveTag() {
        Tag tag = new Tag("Inactive");
        tag.setId(1);
        tag.setIsActive(false);

        when(tagRepository.findById(1)).thenReturn(Optional.of(tag));

        assertThrows(IllegalArgumentException.class, () ->
            userService.updateUserTags(1, Arrays.asList(1))
        );
    }
}