package com.jobtrack.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/** Creates and verifies signed JWTs (HMAC-SHA). */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtUtil {

    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(JwtProperties properties) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(properties.secret());
        } catch (RuntimeException e) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be a Base64 string", e);
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must decode to at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = properties.expirationMs();
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("role", principal.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Returns the email stored in the token. Throws a JwtException if it is invalid, tampered or expired. */
    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Long extractUserId(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .get("uid", Long.class);
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
