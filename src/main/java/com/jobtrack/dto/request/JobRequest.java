package com.jobtrack.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.jobtrack.entity.enums.JobStatus;
import com.jobtrack.entity.enums.JobType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JobRequest(
        @NotNull Long companyId,
        @NotBlank @Size(max = 150) String title,
        @NotBlank String description,
        @Size(max = 100) String location,
        @NotNull JobType jobType,
        @Size(max = 500) String skillsRequired,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal minCgpa,
        @DecimalMin("0.0") BigDecimal salaryLpa,
        LocalDate applicationDeadline,
        LocalDate driveDate,
        JobStatus status) {
}
