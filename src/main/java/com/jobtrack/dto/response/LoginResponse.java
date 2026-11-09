package com.jobtrack.dto.response;

import com.jobtrack.entity.enums.Role;

public record LoginResponse(String token, String tokenType, long expiresInMs,
                            Long userId, String email, String fullName, Role role) {
}
