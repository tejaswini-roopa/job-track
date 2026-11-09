package com.jobtrack.dto.response;

import java.math.BigDecimal;

public record ProfileResponse(Long id, Long userId, String email, String fullName, String phone,
                              String college, String degree, String branch, Integer graduationYear,
                              BigDecimal cgpa, String skills, String resumeUrl) {
}
