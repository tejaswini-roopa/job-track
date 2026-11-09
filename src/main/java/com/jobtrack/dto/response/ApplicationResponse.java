package com.jobtrack.dto.response;

import java.time.LocalDateTime;

import com.jobtrack.entity.enums.ApplicationStatus;

public record ApplicationResponse(Long id, Long studentId, String studentName, String studentEmail,
                                  Long jobId, String jobTitle, String companyName,
                                  ApplicationStatus status, String notes,
                                  LocalDateTime appliedAt, LocalDateTime updatedAt) {
}
