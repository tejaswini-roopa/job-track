package com.jobtrack.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.jobtrack.entity.enums.Role;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtUtilTest {

    // Base64 of a 42 character string (>= 32 bytes)
    private static final String SECRET = "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNDU2";

    private final UserPrincipal principal = new UserPrincipal(7L, "a@b.com", "HASH", "Asha", Role.STUDENT, true);

    @Test
    void generatedTokenCanBeParsed() {
        JwtUtil jwtUtil = new JwtUtil(new JwtProperties(SECRET, 60_000));

        String token = jwtUtil.generateToken(principal);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("a@b.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(7L);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(new JwtProperties(SECRET, 60_000));
        String token = jwtUtil.generateToken(principal);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThatThrownBy(() -> jwtUtil.extractEmail(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(new JwtProperties(SECRET, -1_000));
        String token = jwtUtil.generateToken(principal);

        assertThatThrownBy(() -> jwtUtil.extractEmail(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void weakSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil(new JwtProperties("c2hvcnQ=", 60_000)))
                .isInstanceOf(IllegalStateException.class);
    }
}
