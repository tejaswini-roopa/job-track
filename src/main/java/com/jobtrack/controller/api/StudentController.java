package com.jobtrack.controller.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobtrack.dto.request.UpdateProfileRequest;
import com.jobtrack.dto.response.DashboardResponse;
import com.jobtrack.dto.response.PageResponse;
import com.jobtrack.dto.response.ProfileResponse;
import com.jobtrack.dto.response.SavedJobResponse;
import com.jobtrack.security.CurrentUser;
import com.jobtrack.service.DashboardService;
import com.jobtrack.service.SavedJobService;
import com.jobtrack.service.StudentProfileService;

import jakarta.validation.Valid;

/** Everything a student owns: profile, dashboard and wishlist. */
@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentProfileService profileService;
    private final DashboardService dashboardService;
    private final SavedJobService savedJobService;
    private final CurrentUser currentUser;

    public StudentController(StudentProfileService profileService, DashboardService dashboardService,
                             SavedJobService savedJobService, CurrentUser currentUser) {
        this.profileService = profileService;
        this.dashboardService = dashboardService;
        this.savedJobService = savedJobService;
        this.currentUser = currentUser;
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile() {
        return profileService.getProfile(currentUser.id());
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(currentUser.id(), request);
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return dashboardService.getStudentDashboard(currentUser.id());
    }

    @GetMapping("/saved-jobs")
    public PageResponse<SavedJobResponse> savedJobs(
            @PageableDefault(size = 10, sort = "savedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return savedJobService.list(currentUser.id(), pageable);
    }

    @PostMapping("/saved-jobs/{jobId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SavedJobResponse saveJob(@PathVariable Long jobId) {
        return savedJobService.save(currentUser.id(), jobId);
    }

    @DeleteMapping("/saved-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSavedJob(@PathVariable Long jobId) {
        savedJobService.remove(currentUser.id(), jobId);
    }
}
