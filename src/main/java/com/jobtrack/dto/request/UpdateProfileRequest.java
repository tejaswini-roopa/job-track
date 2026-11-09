package com.jobtrack.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String fullName,
        @Pattern(regexp = "^$|^[0-9+\\-\\s]{7,20}$", message = "invalid phone number") String phone,
        @Size(max = 150) String college,
        @Size(max = 80) String degree,
        @Size(max = 80) String branch,
        @Min(2000) @Max(2100) Integer graduationYear,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal cgpa,
        @Size(max = 500) String skills,
        @Size(max = 255) String resumeUrl) {
}
