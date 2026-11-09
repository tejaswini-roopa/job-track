package com.jobtrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtrack.dto.response.AnalyticsResponse;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.entity.enums.JobStatus;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.repository.JobApplicationRepository;
import com.jobtrack.repository.JobRepository;
import com.jobtrack.repository.UserRepository;
import com.jobtrack.repository.projection.CompanyCount;
import com.jobtrack.repository.projection.MonthlyCount;
import com.jobtrack.repository.projection.StatusCount;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private AnalyticsService service;

    private record Status(ApplicationStatus status, Long total) implements StatusCount {
        public ApplicationStatus getStatus() { return status; }
        public Long getTotal() { return total; }
    }

    private record Company(String name, Long total) implements CompanyCount {
        public String getCompanyName() { return name; }
        public Long getTotal() { return total; }
    }

    private record Month(Integer year, Integer month, Long total) implements MonthlyCount {
        public Integer getPeriodYear() { return year; }
        public Integer getPeriodMonth() { return month; }
        public Long getTotal() { return total; }
    }

    @Test
    void calculatesRatesAndTrends() {
        when(applicationRepository.countByStatusOverall()).thenReturn(List.of(
                new Status(ApplicationStatus.APPLIED, 6L),
                new Status(ApplicationStatus.SELECTED, 2L),
                new Status(ApplicationStatus.REJECTED, 2L)));
        when(applicationRepository.countByCompany()).thenReturn(List.of(new Company("Acme", 7L)));
        when(applicationRepository.countMonthlyOverall()).thenReturn(List.of(new Month(2026, 11, 10L)));
        when(jobRepository.countByStatus(JobStatus.OPEN)).thenReturn(3L);
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(5L);

        AnalyticsResponse r = service.getOverview();

        assertThat(r.totalApplications()).isEqualTo(10);
        assertThat(r.successRate()).isEqualTo(20.0);
        assertThat(r.selectionRate()).isEqualTo(50.0);
        assertThat(r.rejectionRate()).isEqualTo(50.0);
        assertThat(r.totalStudents()).isEqualTo(5);
        assertThat(r.openJobs()).isEqualTo(3);
        assertThat(r.companyStats()).hasSize(1);
        assertThat(r.monthlyTrend().get(0).month()).isEqualTo("2026-11");
        assertThat(r.statusBreakdown()).containsKeys(ApplicationStatus.values());
    }

    @Test
    void emptyDatabaseGivesZeroRatesWithoutDividingByZero() {
        AnalyticsResponse r = service.getOverview();

        assertThat(r.totalApplications()).isZero();
        assertThat(r.successRate()).isZero();
        assertThat(r.selectionRate()).isZero();
        assertThat(r.rejectionRate()).isZero();
    }
}
