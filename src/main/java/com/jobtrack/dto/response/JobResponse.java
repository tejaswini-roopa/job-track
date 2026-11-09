package com.jobtrack.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.jobtrack.entity.enums.JobStatus;
import com.jobtrack.entity.enums.JobType;

public record JobResponse(Long id, Long companyId, String companyName, String title, String description,
                          String location, JobType jobType, String skillsRequired, BigDecimal minCgpa,
                          BigDecimal salaryLpa, LocalDate applicationDeadline, LocalDate driveDate,
                          JobStatus status, LocalDateTime createdAt) {
}
