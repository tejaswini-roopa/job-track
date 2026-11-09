package com.jobtrack.controller.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobtrack.dto.request.JobSearchCriteria;
import com.jobtrack.dto.response.JobResponse;
import com.jobtrack.dto.response.PageResponse;
import com.jobtrack.entity.enums.JobType;
import com.jobtrack.security.CurrentUser;
import com.jobtrack.service.JobService;

/** Job browsing for students. Example: GET /api/jobs?skills=java,spring&eligibleOnly=true&sort=salaryLpa,desc */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final CurrentUser currentUser;

    public JobController(JobService jobService, CurrentUser currentUser) {
        this.jobService = jobService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<JobResponse> search(
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) Boolean eligibleOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        JobSearchCriteria criteria = new JobSearchCriteria(company, role, location, skills, jobType, eligibleOnly);
        return jobService.searchForStudent(currentUser.id(), criteria, pageable);
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable Long id) {
        return jobService.get(id);
    }
}
