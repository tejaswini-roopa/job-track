package com.jobtrack.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.jobtrack.entity.enums.Role;
import com.jobtrack.exception.UnauthorizedException;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsIdOfAuthenticatedUser() {
        UserPrincipal principal = new UserPrincipal(42L, "a@b.com", "HASH", "Asha", Role.STUDENT, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThat(currentUser.id()).isEqualTo(42L);
    }

    @Test
    void throwsWhenNobodyIsAuthenticated() {
        assertThatThrownBy(currentUser::id).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void throwsForAnonymousUser() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(currentUser::id).isInstanceOf(UnauthorizedException.class);
    }
}
