package com.jobtrack.dto.request;

import com.jobtrack.entity.enums.ApplicationStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotNull ApplicationStatus status,
        @Size(max = 255) String remarks) {
}
