package com.jobtrack.dto.response;

import java.time.LocalDateTime;

public record SavedJobResponse(Long id, JobResponse job, LocalDateTime savedAt) {
}
