package com.jobtrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtrack.dto.request.JobRequest;
import com.jobtrack.dto.response.JobResponse;
import com.jobtrack.entity.Company;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.enums.JobStatus;
import com.jobtrack.entity.enums.JobType;
import com.jobtrack.exception.BusinessRuleException;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.repository.CompanyRepository;
import com.jobtrack.repository.JobApplicationRepository;
import com.jobtrack.repository.JobRepository;
import com.jobtrack.repository.StudentProfileRepository;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @Mock
    private JobApplicationRepository applicationRepository;
    @InjectMocks
    private JobService service;

    private Company company() {
        Company c = new Company();
        c.setId(1L);
        c.setName("Acme");
        return c;
    }

    @Test
    void createAppliesDefaultsAndTrimsTitle() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company()));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        JobRequest request = new JobRequest(1L, "  Java Dev ", "desc", "Pune", JobType.FULL_TIME,
                "Java", null, null, null, null, null);

        JobResponse response = service.create(request);

        assertThat(response.title()).isEqualTo("Java Dev");
        assertThat(response.status()).isEqualTo(JobStatus.OPEN);
        assertThat(response.minCgpa()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.companyName()).isEqualTo("Acme");
    }

    @Test
    void createRejectsDriveDateBeforeDeadline() {
        JobRequest request = new JobRequest(1L, "Dev", "desc", null, JobType.FULL_TIME, null, null, null,
                LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 1), null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BusinessRuleException.class);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void deleteIsBlockedWhenApplicationsExist() {
        Job job = new Job();
        job.setId(5L);
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Close it");
        verify(jobRepository, never()).delete(any());
    }

    @Test
    void updatingUnknownJobThrowsNotFound() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());
        JobRequest request = new JobRequest(1L, "Dev", "desc", null, JobType.FULL_TIME, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.update(99L, request)).isInstanceOf(ResourceNotFoundException.class);
    }
}
