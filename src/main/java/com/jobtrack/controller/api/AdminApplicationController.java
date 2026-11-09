package com.jobtrack.controller.api;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobtrack.dto.request.InterviewRequest;
import com.jobtrack.dto.request.StatusUpdateRequest;
import com.jobtrack.dto.response.AnalyticsResponse;
import com.jobtrack.dto.response.ApplicationResponse;
import com.jobtrack.dto.response.HistoryResponse;
import com.jobtrack.dto.response.InterviewResponse;
import com.jobtrack.dto.response.PageResponse;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.security.CurrentUser;
import com.jobtrack.service.AnalyticsService;
import com.jobtrack.service.ApplicationService;

import jakarta.validation.Valid;

/** Recruitment management and analytics for admins. */
@RestController
@RequestMapping("/api/admin")
public class AdminApplicationController {

    private final ApplicationService applicationService;
    private final AnalyticsService analyticsService;
    private final CurrentUser currentUser;

    public AdminApplicationController(ApplicationService applicationService,
                                      AnalyticsService analyticsService,
                                      CurrentUser currentUser) {
        this.applicationService = applicationService;
        this.analyticsService = analyticsService;
        this.currentUser = currentUser;
    }

    @GetMapping("/applications")
    public PageResponse<ApplicationResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return applicationService.listAll(status, pageable);
    }

    @GetMapping("/applications/{id}/history")
    public List<HistoryResponse> history(@PathVariable Long id) {
        return applicationService.getHistory(id);
    }

    @PatchMapping("/applications/{id}/status")
    public ApplicationResponse updateStatus(@PathVariable Long id,
                                            @Valid @RequestBody StatusUpdateRequest request) {
        return applicationService.updateStatus(id, currentUser.id(), request);
    }

    @PostMapping("/applications/{id}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewResponse scheduleInterview(@PathVariable Long id,
                                               @Valid @RequestBody InterviewRequest request) {
        return applicationService.scheduleInterview(id, request);
    }

    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return analyticsService.getOverview();
    }
}
