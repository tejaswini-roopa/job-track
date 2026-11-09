package com.jobtrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtrack.dto.request.UpdateProfileRequest;
import com.jobtrack.dto.response.ProfileResponse;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.entity.User;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.repository.StudentProfileRepository;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceTest {

    @Mock
    private StudentProfileRepository profileRepository;
    @InjectMocks
    private StudentProfileService service;

    @Test
    void updateChangesProfileAndUserName() {
        User user = new User();
        user.setId(10L);
        user.setEmail("a@b.com");
        user.setFullName("Old Name");
        StudentProfile profile = new StudentProfile();
        profile.setId(1L);
        profile.setUser(user);
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));

        ProfileResponse response = service.updateProfile(10L, new UpdateProfileRequest(
                "  New Name ", "9876543210", "ABC Institute", "B.Tech", "CSE", 2026,
                new BigDecimal("8.50"), "Java, Spring", null));

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.cgpa()).isEqualByComparingTo("8.50");
        assertThat(profile.getCollege()).isEqualTo("ABC Institute");
        assertThat(user.getFullName()).isEqualTo("New Name");
    }

    @Test
    void missingProfileThrowsNotFound() {
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(10L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
