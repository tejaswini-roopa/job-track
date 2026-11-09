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

import com.jobtrack.dto.request.ApplyRequest;
import com.jobtrack.dto.request.NotesRequest;
import com.jobtrack.dto.response.ApplicationResponse;
import com.jobtrack.dto.response.HistoryResponse;
import com.jobtrack.dto.response.PageResponse;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.security.CurrentUser;
import com.jobtrack.service.ApplicationService;

import jakarta.validation.Valid;

/** A student's own applications. */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CurrentUser currentUser;

    public ApplicationController(ApplicationService applicationService, CurrentUser currentUser) {
        this.applicationService = applicationService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@Valid @RequestBody ApplyRequest request) {
        return applicationService.apply(currentUser.id(), request.jobId());
    }

    @GetMapping
    public PageResponse<ApplicationResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return applicationService.listMine(currentUser.id(), status, pageable);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable Long id) {
        return applicationService.getMine(currentUser.id(), id);
    }

    @PatchMapping("/{id}/notes")
    public ApplicationResponse updateNotes(@PathVariable Long id, @Valid @RequestBody NotesRequest request) {
        return applicationService.updateNotes(currentUser.id(), id, request.notes());
    }

    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@PathVariable Long id) {
        return applicationService.getMyHistory(currentUser.id(), id);
    }
}
