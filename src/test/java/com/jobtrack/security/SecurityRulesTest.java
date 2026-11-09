package com.jobtrack.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.jobtrack.config.SecurityConfig;
import com.jobtrack.controller.api.AdminApplicationController;
import com.jobtrack.controller.api.ApplicationController;
import com.jobtrack.dto.response.AnalyticsResponse;
import com.jobtrack.service.AnalyticsService;
import com.jobtrack.service.ApplicationService;

@WebMvcTest({AdminApplicationController.class, ApplicationController.class})
@Import(SecurityConfig.class)
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ApplicationService applicationService;
    @MockBean
    private AnalyticsService analyticsService;
    @MockBean
    private CurrentUser currentUser;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void anonymousRequestGets401Json() throws Exception {
        mockMvc.perform(get("/api/applications/1"))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.status").value(401))
               .andExpect(jsonPath("$.path").value("/api/applications/1"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotUseAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/analytics"))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUseAdminEndpoints() throws Exception {
        when(analyticsService.getOverview()).thenReturn(
                new AnalyticsResponse(0, 0, 0, 0, 0, 0, 0, 0, Map.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/admin/analytics"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotUseStudentEndpoints() throws Exception {
        mockMvc.perform(get("/api/applications/1"))
               .andExpect(status().isForbidden());
    }
}
