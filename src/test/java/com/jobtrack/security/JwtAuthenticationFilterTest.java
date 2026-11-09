package com.jobtrack.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.jobtrack.entity.enums.Role;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNDU2";

    private final JwtUtil jwtUtil = new JwtUtil(new JwtProperties(SECRET, 60_000));
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void run(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void validTokenAuthenticatesTheRequest() throws Exception {
        UserPrincipal user = new UserPrincipal(7L, "a@b.com", "HASH", "Asha", Role.STUDENT, true);
        when(userDetailsService.loadUserByUsername("a@b.com")).thenReturn(user);

        run("Bearer " + jwtUtil.generateToken(user));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(((UserPrincipal) authentication.getPrincipal()).getId()).isEqualTo(7L);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_STUDENT");
    }

    @Test
    void invalidTokenLeavesRequestAnonymous() throws Exception {
        run("Bearer not.a.real.token");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void disabledUserIsNotAuthenticated() throws Exception {
        UserPrincipal disabled = new UserPrincipal(7L, "a@b.com", "HASH", "Asha", Role.STUDENT, false);
        when(userDetailsService.loadUserByUsername("a@b.com")).thenReturn(disabled);

        run("Bearer " + jwtUtil.generateToken(disabled));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
