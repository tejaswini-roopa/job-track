package com.jobtrack.dto.response;

import java.time.LocalDateTime;

import com.jobtrack.entity.enums.Role;

public record UserResponse(Long id, String email, String fullName, Role role,
                           boolean enabled, LocalDateTime createdAt) {
}
