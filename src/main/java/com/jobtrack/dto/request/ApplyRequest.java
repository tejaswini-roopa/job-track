package com.jobtrack.dto.request;

import jakarta.validation.constraints.NotNull;

public record ApplyRequest(@NotNull Long jobId) {
}
