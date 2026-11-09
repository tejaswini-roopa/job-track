package com.jobtrack.dto.request;

import java.time.LocalDateTime;

import com.jobtrack.entity.enums.InterviewMode;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewRequest(
        @NotBlank @Size(max = 80) String roundName,
        @NotNull @Future LocalDateTime scheduledAt,
        @NotNull InterviewMode mode,
        @Size(max = 255) String locationOrLink) {
}
