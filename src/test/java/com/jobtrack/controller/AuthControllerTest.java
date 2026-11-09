package com.jobtrack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jobtrack.config.SecurityConfig;
import com.jobtrack.controller.api.AuthController;
import com.jobtrack.dto.request.LoginRequest;
import com.jobtrack.dto.request.RegisterRequest;
import com.jobtrack.dto.response.LoginResponse;
import com.jobtrack.dto.response.UserResponse;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.security.JwtUtil;
import com.jobtrack.security.UserDetailsServiceImpl;
import com.jobtrack.service.AuthService;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AuthService authService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private static final String VALID_BODY =
            "{\"fullName\":\"Asha\",\"email\":\"asha@test.com\",\"password\":\"Passw0rd!\"}";

    @Test
    void registerReturns201() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(
                new UserResponse(1L, "asha@test.com", "Asha", Role.STUDENT, true, LocalDateTime.now()));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.email").value("asha@test.com"))
               .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        String body = "{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("Validation failed"))
               .andExpect(jsonPath("$.fieldErrors.email").exists())
               .andExpect(jsonPath("$.fieldErrors.password").exists())
               .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("An account with this email already exists"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{bad json"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new LoginResponse("jwt-token", "Bearer", 86400000L, 1L, "asha@test.com", "Asha", Role.STUDENT));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@test.com\",\"password\":\"Passw0rd!\"}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token").value("jwt-token"))
               .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void wrongCredentialsReturn401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@test.com\",\"password\":\"wrong\"}"))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.message").value("Bad credentials"));
    }
}
