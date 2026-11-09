package com.jobtrack.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds app.jwt.* from application.yml (values come from environment variables). */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
}
