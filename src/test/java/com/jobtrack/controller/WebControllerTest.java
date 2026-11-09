package com.jobtrack.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jobtrack.config.SecurityConfig;
import com.jobtrack.controller.web.WebController;
import com.jobtrack.security.JwtUtil;
import com.jobtrack.security.UserDetailsServiceImpl;

/** Renders every Thymeleaf template, so a broken fragment or expression fails the build. */
@WebMvcTest(WebController.class)
@Import(SecurityConfig.class)
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/login", "/register", "/dashboard", "/jobs", "/jobs/5",
                            "/applications", "/profile", "/admin"})
    void everyPageRendersPublicly(String path) throws Exception {
        mockMvc.perform(get(path))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
               .andExpect(content().string(containsString("JobTrack")));
    }

    @Test
    void jobDetailsPageCarriesTheJobId() throws Exception {
        mockMvc.perform(get("/jobs/7"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("data-job-id=\"7\"")));
    }
}
