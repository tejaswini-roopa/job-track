package com.jobtrack.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtrack.exception.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter-level failures happen before Spring MVC, so @RestControllerAdvice cannot see them.
 * This writes the same ApiError JSON by hand.
 */
final class SecurityResponses {

    private SecurityResponses() {
    }

    static void write(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ApiError(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(), null));
    }
}
