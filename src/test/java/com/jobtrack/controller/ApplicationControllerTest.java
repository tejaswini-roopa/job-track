package com.jobtrack.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.jobtrack.config.SecurityConfig;
import com.jobtrack.controller.api.ApplicationController;
import com.jobtrack.dto.response.ApplicationResponse;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.exception.BusinessRuleException;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.exception.UnauthorizedException;
import com.jobtrack.security.CurrentUser;
import com.jobtrack.security.JwtUtil;
import com.jobtrack.security.UserDetailsServiceImpl;
import com.jobtrack.service.ApplicationService;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "STUDENT")
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ApplicationService applicationService;
    @MockBean
    private CurrentUser currentUser;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void identifyUser() {
        when(currentUser.id()).thenReturn(10L);
    }

    private ApplicationResponse sample() {
        return new ApplicationResponse(1L, 1L, "Asha", "asha@test.com", 5L, "Java Developer",
                "Acme", ApplicationStatus.APPLIED, null, null, null);
    }

    @Test
    void applyReturns201() throws Exception {
        when(applicationService.apply(10L, 5L)).thenReturn(sample());

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":5}"))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.status").value("APPLIED"))
               .andExpect(jsonPath("$.companyName").value("Acme"));
    }

    @Test
    void applyWithoutJobIdReturns400() throws Exception {
        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content("{}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.fieldErrors.jobId").exists());
    }

    @Test
    void duplicateApplicationReturns409() throws Exception {
        when(applicationService.apply(10L, 5L))
                .thenThrow(new DuplicateResourceException("You have already applied to this job"));

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":5}"))
               .andExpect(status().isConflict());
    }

    @Test
    void businessRuleViolationReturns422() throws Exception {
        when(applicationService.apply(10L, 5L))
                .thenThrow(new BusinessRuleException("The application deadline has passed"));

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":5}"))
               .andExpect(status().isUnprocessableEntity())
               .andExpect(jsonPath("$.message").value("The application deadline has passed"));
    }

    @Test
    void unknownApplicationReturns404() throws Exception {
        when(applicationService.getMine(10L, 99L)).thenThrow(new ResourceNotFoundException("Application", 99L));

        mockMvc.perform(get("/api/applications/99"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.path").value("/api/applications/99"));
    }

    @Test
    void missingIdentityReturns401() throws Exception {
        when(currentUser.id()).thenThrow(new UnauthorizedException("Authentication is required"));

        mockMvc.perform(get("/api/applications/1"))
               .andExpect(status().isUnauthorized());
    }
}
