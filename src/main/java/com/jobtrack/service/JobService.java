package com.jobtrack.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtrack.dto.request.JobRequest;
import com.jobtrack.dto.request.JobSearchCriteria;
import com.jobtrack.dto.response.JobResponse;
import com.jobtrack.dto.response.PageResponse;
import com.jobtrack.entity.Company;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.entity.enums.JobStatus;
import com.jobtrack.exception.BusinessRuleException;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.mapper.EntityMapper;
import com.jobtrack.repository.CompanyRepository;
import com.jobtrack.repository.JobApplicationRepository;
import com.jobtrack.repository.JobRepository;
import com.jobtrack.repository.JobSpecifications;
import com.jobtrack.repository.StudentProfileRepository;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final StudentProfileRepository profileRepository;
    private final JobApplicationRepository applicationRepository;

    public JobService(JobRepository jobRepository,
                      CompanyRepository companyRepository,
                      StudentProfileRepository profileRepository,
                      JobApplicationRepository applicationRepository) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.profileRepository = profileRepository;
        this.applicationRepository = applicationRepository;
    }

    /** Student view: open jobs only, optionally filtered by the student's CGPA. */
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> searchForStudent(Long userId, JobSearchCriteria criteria, Pageable pageable) {
        BigDecimal cgpa = null;
        if (Boolean.TRUE.equals(criteria.eligibleOnly())) {
            cgpa = profileRepository.findByUserId(userId)
                    .map(StudentProfile::getCgpa)
                    .orElse(BigDecimal.ZERO);
            if (cgpa == null) {
                cgpa = BigDecimal.ZERO;
            }
        }
        Page<Job> page = jobRepository.findAll(JobSpecifications.build(criteria, true, cgpa), pageable);
        return PageResponse.from(page.map(EntityMapper::toJobResponse));
    }

    /** Admin view: every job, including closed ones. */
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> searchAll(JobSearchCriteria criteria, Pageable pageable) {
        Page<Job> page = jobRepository.findAll(JobSpecifications.build(criteria, false, null), pageable);
        return PageResponse.from(page.map(EntityMapper::toJobResponse));
    }

    @Transactional(readOnly = true)
    public JobResponse get(Long id) {
        return EntityMapper.toJobResponse(find(id));
    }

    @Transactional
    public JobResponse create(JobRequest request) {
        Job job = new Job();
        apply(job, request);
        return EntityMapper.toJobResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse update(Long id, JobRequest request) {
        Job job = find(id);
        apply(job, request);
        return EntityMapper.toJobResponse(job);
    }

    /** Jobs that already have applications are protected: close them instead of deleting. */
    @Transactional
    public void delete(Long id) {
        Job job = find(id);
        if (applicationRepository.existsByJobId(id)) {
            throw new BusinessRuleException(
                    "This job already has applications. Close it instead of deleting it");
        }
        jobRepository.delete(job);
    }

    @Transactional
    public JobResponse setStatus(Long id, JobStatus status) {
        Job job = find(id);
        job.setStatus(status);
        return EntityMapper.toJobResponse(job);
    }

    private Job find(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));
    }

    private void apply(Job job, JobRequest r) {
        if (r.applicationDeadline() != null && r.driveDate() != null
                && r.driveDate().isBefore(r.applicationDeadline())) {
            throw new BusinessRuleException("Drive date cannot be before the application deadline");
        }
        Company company = companyRepository.findById(r.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", r.companyId()));
        job.setCompany(company);
        job.setTitle(r.title().trim());
        job.setDescription(r.description());
        job.setLocation(r.location());
        job.setJobType(r.jobType());
        job.setSkillsRequired(r.skillsRequired());
        job.setMinCgpa(r.minCgpa() == null ? BigDecimal.ZERO : r.minCgpa());
        job.setSalaryLpa(r.salaryLpa());
        job.setApplicationDeadline(r.applicationDeadline());
        job.setDriveDate(r.driveDate());
        job.setStatus(r.status() == null ? JobStatus.OPEN : r.status());
    }
}
