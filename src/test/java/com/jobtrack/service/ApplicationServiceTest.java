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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtrack.dto.request.StatusUpdateRequest;
import com.jobtrack.dto.response.ApplicationResponse;
import com.jobtrack.entity.ApplicationHistory;
import com.jobtrack.entity.Company;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.JobApplication;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.entity.User;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.entity.enums.JobStatus;
import com.jobtrack.exception.BusinessRuleException;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.repository.ApplicationHistoryRepository;
import com.jobtrack.repository.InterviewRepository;
import com.jobtrack.repository.JobApplicationRepository;
import com.jobtrack.repository.JobRepository;
import com.jobtrack.repository.StudentProfileRepository;
import com.jobtrack.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long STUDENT_ID = 1L;
    private static final Long JOB_ID = 5L;

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationHistoryRepository historyRepository;
    @Mock
    private InterviewRepository interviewRepository;
    @InjectMocks
    private ApplicationService service;

    private StudentProfile student;
    private Job job;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);
        user.setFullName("Asha");
        user.setEmail("asha@test.com");

        student = new StudentProfile();
        student.setId(STUDENT_ID);
        student.setUser(user);
        student.setCgpa(new BigDecimal("8.00"));

        Company company = new Company();
        company.setId(2L);
        company.setName("Acme");

        job = new Job();
        job.setId(JOB_ID);
        job.setCompany(company);
        job.setTitle("Java Developer");
        job.setMinCgpa(new BigDecimal("7.00"));
        job.setStatus(JobStatus.OPEN);
        job.setApplicationDeadline(LocalDate.now().plusDays(5));
    }

    private void stubStudentAndJob() {
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(student));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
    }

    @Test
    void applySucceedsAndWritesHistory() {
        stubStudentAndJob();
        when(applicationRepository.existsByStudentIdAndJobId(STUDENT_ID, JOB_ID)).thenReturn(false);
        when(applicationRepository.saveAndFlush(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = service.apply(USER_ID, JOB_ID);

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.companyName()).isEqualTo("Acme");
        ArgumentCaptor<ApplicationHistory> captor = ArgumentCaptor.forClass(ApplicationHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getOldStatus()).isNull();
        assertThat(captor.getValue().getNewStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void applyRejectsDuplicate() {
        stubStudentAndJob();
        when(applicationRepository.existsByStudentIdAndJobId(STUDENT_ID, JOB_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(USER_ID, JOB_ID))
                .isInstanceOf(DuplicateResourceException.class);
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void applyRejectsIneligibleStudent() {
        student.setCgpa(new BigDecimal("6.00"));
        stubStudentAndJob();

        assertThatThrownBy(() -> service.apply(USER_ID, JOB_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CGPA");
    }

    @Test
    void applyRejectsAfterDeadline() {
        job.setApplicationDeadline(LocalDate.now().minusDays(1));
        stubStudentAndJob();

        assertThatThrownBy(() -> service.apply(USER_ID, JOB_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deadline");
    }

    private JobApplication existingApplication(ApplicationStatus status) {
        JobApplication app = new JobApplication();
        app.setId(100L);
        app.setStudent(student);
        app.setJob(job);
        app.setStatus(status);
        return app;
    }

    @Test
    void updateStatusAllowsValidTransitionAndRecordsHistory() {
        when(applicationRepository.findById(100L))
                .thenReturn(Optional.of(existingApplication(ApplicationStatus.APPLIED)));

        ApplicationResponse response = service.updateStatus(100L, 1L,
                new StatusUpdateRequest(ApplicationStatus.UNDER_REVIEW, "Shortlisted"));

        assertThat(response.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        ArgumentCaptor<ApplicationHistory> captor = ArgumentCaptor.forClass(ApplicationHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getOldStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(captor.getValue().getNewStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(captor.getValue().getRemarks()).isEqualTo("Shortlisted");
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        when(applicationRepository.findById(100L))
                .thenReturn(Optional.of(existingApplication(ApplicationStatus.APPLIED)));

        assertThatThrownBy(() -> service.updateStatus(100L, 1L,
                new StatusUpdateRequest(ApplicationStatus.SELECTED, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid status change");
        verify(historyRepository, never()).save(any());
    }
}
