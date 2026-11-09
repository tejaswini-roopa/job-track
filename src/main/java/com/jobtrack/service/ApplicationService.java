package com.jobtrack.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtrack.dto.request.InterviewRequest;
import com.jobtrack.dto.request.StatusUpdateRequest;
import com.jobtrack.dto.response.ApplicationResponse;
import com.jobtrack.dto.response.HistoryResponse;
import com.jobtrack.dto.response.InterviewResponse;
import com.jobtrack.dto.response.PageResponse;
import com.jobtrack.entity.ApplicationHistory;
import com.jobtrack.entity.Interview;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.JobApplication;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.entity.User;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.entity.enums.JobStatus;
import com.jobtrack.exception.BusinessRuleException;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.mapper.EntityMapper;
import com.jobtrack.repository.ApplicationHistoryRepository;
import com.jobtrack.repository.InterviewRepository;
import com.jobtrack.repository.JobApplicationRepository;
import com.jobtrack.repository.JobRepository;
import com.jobtrack.repository.StudentProfileRepository;
import com.jobtrack.repository.UserRepository;

@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ApplicationHistoryRepository historyRepository;
    private final InterviewRepository interviewRepository;

    public ApplicationService(JobApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              StudentProfileRepository profileRepository,
                              UserRepository userRepository,
                              ApplicationHistoryRepository historyRepository,
                              InterviewRepository interviewRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.interviewRepository = interviewRepository;
    }

    // ---------------- student operations ----------------

    /** Application + its first history row are saved atomically. */
    @Transactional
    public ApplicationResponse apply(Long userId, Long jobId) {
        StudentProfile student = findProfile(userId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        validateJobIsOpen(job);
        validateEligibility(student, job);
        if (applicationRepository.existsByStudentIdAndJobId(student.getId(), jobId)) {
            throw new DuplicateResourceException("You have already applied to this job");
        }

        JobApplication application = new JobApplication();
        application.setStudent(student);
        application.setJob(job);
        try {
            application = applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            // Two simultaneous requests can both pass the check above; the DB unique key is the final guard.
            throw new DuplicateResourceException("You have already applied to this job");
        }

        recordHistory(application, null, ApplicationStatus.APPLIED,
                userRepository.getReferenceById(userId), "Application submitted");
        return EntityMapper.toApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listMine(Long userId, ApplicationStatus status, Pageable pageable) {
        Long studentId = findProfile(userId).getId();
        Page<JobApplication> page = (status == null)
                ? applicationRepository.findByStudentId(studentId, pageable)
                : applicationRepository.findByStudentIdAndStatus(studentId, status, pageable);
        return PageResponse.from(page.map(EntityMapper::toApplicationResponse));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getMine(Long userId, Long applicationId) {
        return EntityMapper.toApplicationResponse(findOwned(userId, applicationId));
    }

    @Transactional
    public ApplicationResponse updateNotes(Long userId, Long applicationId, String notes) {
        JobApplication application = findOwned(userId, applicationId);
        application.setNotes(notes);
        return EntityMapper.toApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getMyHistory(Long userId, Long applicationId) {
        findOwned(userId, applicationId);
        return historyFor(applicationId);
    }

    // ---------------- admin operations ----------------

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listAll(ApplicationStatus status, Pageable pageable) {
        Page<JobApplication> page = (status == null)
                ? applicationRepository.findAll(pageable)
                : applicationRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(EntityMapper::toApplicationResponse));
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getHistory(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", applicationId);
        }
        return historyFor(applicationId);
    }

    /** Enforces the state machine and writes the audit trail in the same transaction. */
    @Transactional
    public ApplicationResponse updateStatus(Long applicationId, Long adminUserId, StatusUpdateRequest request) {
        JobApplication application = findById(applicationId);
        ApplicationStatus current = application.getStatus();
        ApplicationStatus next = request.status();

        if (current == next) {
            throw new BusinessRuleException("Application is already in status " + current);
        }
        if (!current.canTransitionTo(next)) {
            throw new BusinessRuleException("Invalid status change: " + current + " -> " + next);
        }

        application.setStatus(next);
        recordHistory(application, current, next,
                userRepository.getReferenceById(adminUserId), request.remarks());
        return EntityMapper.toApplicationResponse(application);
    }

    @Transactional
    public InterviewResponse scheduleInterview(Long applicationId, InterviewRequest request) {
        JobApplication application = findById(applicationId);
        if (application.getStatus().isTerminal()) {
            throw new BusinessRuleException("Cannot schedule an interview for a "
                    + application.getStatus() + " application");
        }
        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setRoundName(request.roundName().trim());
        interview.setScheduledAt(request.scheduledAt());
        interview.setMode(request.mode());
        interview.setLocationOrLink(request.locationOrLink());
        return EntityMapper.toInterviewResponse(interviewRepository.save(interview));
    }

    // ---------------- helpers ----------------

    private StudentProfile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for this user"));
    }

    private JobApplication findById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    }

    /** Returns 404 (not 403) for someone else's application so its existence is not leaked. */
    private JobApplication findOwned(Long userId, Long applicationId) {
        Long studentId = findProfile(userId).getId();
        return applicationRepository.findByIdAndStudentId(applicationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
    }

    private void validateJobIsOpen(Job job) {
        if (job.getStatus() != JobStatus.OPEN) {
            throw new BusinessRuleException("This job is closed for applications");
        }
        if (job.getApplicationDeadline() != null && LocalDate.now().isAfter(job.getApplicationDeadline())) {
            throw new BusinessRuleException("The application deadline has passed");
        }
    }

    private void validateEligibility(StudentProfile student, Job job) {
        BigDecimal required = job.getMinCgpa() == null ? BigDecimal.ZERO : job.getMinCgpa();
        BigDecimal actual = student.getCgpa() == null ? BigDecimal.ZERO : student.getCgpa();
        if (actual.compareTo(required) < 0) {
            throw new BusinessRuleException("A minimum CGPA of " + required + " is required for this job");
        }
    }

    private void recordHistory(JobApplication application, ApplicationStatus oldStatus,
                               ApplicationStatus newStatus, User changedBy, String remarks) {
        ApplicationHistory history = new ApplicationHistory();
        history.setApplication(application);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setRemarks(remarks);
        historyRepository.save(history);
    }

    private List<HistoryResponse> historyFor(Long applicationId) {
        return historyRepository.findByApplicationIdOrderByChangedAtDesc(applicationId).stream()
                .map(EntityMapper::toHistoryResponse)
                .toList();
    }
}
