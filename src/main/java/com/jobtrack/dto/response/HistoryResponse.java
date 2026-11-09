package com.jobtrack.dto.response;

import java.time.LocalDateTime;

import com.jobtrack.entity.enums.ApplicationStatus;

public record HistoryResponse(Long id, ApplicationStatus oldStatus, ApplicationStatus newStatus,
                              String changedBy, String remarks, LocalDateTime changedAt) {
}
