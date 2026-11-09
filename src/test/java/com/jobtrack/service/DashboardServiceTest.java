package com.jobtrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtrack.dto.response.DashboardResponse;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.repository.InterviewRepository;
import com.jobtrack.repository.JobApplicationRepository;
import com.jobtrack.repository.StudentProfileRepository;
import com.jobtrack.repository.projection.StatusCount;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @InjectMocks
    private DashboardService service;

    private record Status(ApplicationStatus status, Long total) implements StatusCount {
        public ApplicationStatus getStatus() { return status; }
        public Long getTotal() { return total; }
    }

    @Test
    void summarisesApplicationsByStatus() {
        StudentProfile profile = new StudentProfile();
        profile.setId(1L);
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(applicationRepository.countByStatusForStudent(1L)).thenReturn(List.of(
                new Status(ApplicationStatus.APPLIED, 2L),
                new Status(ApplicationStatus.INTERVIEW, 1L),
                new Status(ApplicationStatus.SELECTED, 1L)));

        DashboardResponse d = service.getStudentDashboard(10L);

        assertThat(d.totalApplications()).isEqualTo(4);
        assertThat(d.interview()).isEqualTo(1);
        assertThat(d.selected()).isEqualTo(1);
        assertThat(d.rejected()).isZero();
        assertThat(d.underReview()).isZero();
        assertThat(d.statusBreakdown()).hasSize(ApplicationStatus.values().length);
        assertThat(d.upcomingInterviews()).isEmpty();
    }

    @Test
    void missingProfileThrowsNotFound() {
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentDashboard(10L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
