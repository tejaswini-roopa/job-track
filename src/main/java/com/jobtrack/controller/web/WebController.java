package com.jobtrack.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Serves the Thymeleaf page shells. They are public HTML; the data on them is loaded by
 * JavaScript from the secured REST API, and each page checks the user's role on load.
 */
@Controller
public class WebController {

    @GetMapping("/")
    public String landing() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/jobs")
    public String jobs() {
        return "jobs";
    }

    @GetMapping("/jobs/{id}")
    public String jobDetails(@PathVariable Long id, Model model) {
        model.addAttribute("jobId", id);
        return "job-details";
    }

    @GetMapping("/applications")
    public String applications() {
        return "applications";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}
