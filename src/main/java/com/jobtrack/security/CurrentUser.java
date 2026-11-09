package com.jobtrack.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.jobtrack.exception.UnauthorizedException;

/** Gives controllers the id of the authenticated user, taken from the SecurityContext (the JWT). */
@Component
public class CurrentUser {

    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new UnauthorizedException("Authentication is required");
    }
}
