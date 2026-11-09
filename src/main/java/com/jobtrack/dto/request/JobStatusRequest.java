package com.jobtrack.dto.request;

import com.jobtrack.entity.enums.JobStatus;

import jakarta.validation.constraints.NotNull;

public record JobStatusRequest(@NotNull JobStatus status) {
}
