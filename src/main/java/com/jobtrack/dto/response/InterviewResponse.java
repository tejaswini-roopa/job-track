package com.jobtrack.dto.response;

import java.time.LocalDateTime;

import com.jobtrack.entity.enums.InterviewMode;

public record InterviewResponse(Long id, Long applicationId, String jobTitle, String companyName,
                                String roundName, LocalDateTime scheduledAt, InterviewMode mode,
                                String locationOrLink) {
}
