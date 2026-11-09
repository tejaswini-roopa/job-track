package com.jobtrack.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/**
 * Boots the whole application (real security, services, repositories, H2 database)
 * and drives it through HTTP exactly like Postman would.
 */
@SpringBootTest(properties = {
        "app.admin.email=admin@test.com",
        "app.admin.password=AdminPass1"
})
@AutoConfigureMockMvc
class JobTrackFlowIntegrationTest {

    private static final String PASSWORD = "Passw0rd!";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    // ---------- helpers ----------

    private ResultActions send(MockHttpServletRequestBuilder builder, String token) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions postJson(String url, String token, String json) throws Exception {
        return send(post(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private ResultActions putJson(String url, String token, String json) throws Exception {
        return send(put(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private ResultActions patchJson(String url, String token, String json) throws Exception {
        return send(patch(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String uniqueEmail() {
        return "student" + SEQ.incrementAndGet() + "@test.com";
    }

    private String loginToken(String email, String password) throws Exception {
        String body = postJson("/api/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String adminToken() throws Exception {
        return loginToken("admin@test.com", "AdminPass1");
    }

    /** Registers a student, logs in, and optionally sets the CGPA. Returns the JWT. */
    private String registerStudent(String email, String cgpa) throws Exception {
        postJson("/api/auth/register", null,
                "{\"fullName\":\"Test Student\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
                .andExpect(status().isCreated());
        String token = loginToken(email, PASSWORD);
        if (cgpa != null) {
            putJson("/api/student/profile", token, "{\"fullName\":\"Test Student\",\"cgpa\":%s}".formatted(cgpa))
                    .andExpect(status().isOk());
        }
        return token;
    }

    private long createOpenJob(String adminToken, String minCgpa) throws Exception {
        int n = SEQ.incrementAndGet();
        long companyId = idOf(postJson("/api/admin/companies", adminToken,
                "{\"name\":\"Company %d\"}".formatted(n)).andExpect(status().isCreated()));
        String job = """
                {"companyId":%d,"title":"Java Developer %d","description":"Build services","location":"Pune",
                 "jobType":"FULL_TIME","skillsRequired":"Java, Spring","minCgpa":%s,"salaryLpa":6.5,
                 "applicationDeadline":"%s"}
                """.formatted(companyId, n, minCgpa, LocalDate.now().plusDays(10));
        return idOf(postJson("/api/admin/jobs", adminToken, job).andExpect(status().isCreated()));
    }

    // ---------- tests ----------

    @Test
    void fullApplicationLifecycle() throws Exception {
        String admin = adminToken();
        long jobId = createOpenJob(admin, "7.0");
        String student = registerStudent(uniqueEmail(), "8.4");

        send(get("/api/jobs").param("role", "java developer"), student)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + jobId + ")]").isNotEmpty());

        long applicationId = idOf(postJson("/api/applications", student, "{\"jobId\":" + jobId + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED")));

        postJson("/api/applications", student, "{\"jobId\":" + jobId + "}")
                .andExpect(status().isConflict());

        patchJson("/api/admin/applications/" + applicationId + "/status", admin, "{\"status\":\"SELECTED\"}")
                .andExpect(status().isUnprocessableEntity());
        patchJson("/api/admin/applications/" + applicationId + "/status", admin,
                "{\"status\":\"UNDER_REVIEW\",\"remarks\":\"Shortlisted\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        send(get("/api/applications/" + applicationId + "/history"), student)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        send(get("/api/student/dashboard"), student)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(1))
                .andExpect(jsonPath("$.underReview").value(1));

        send(get("/api/admin/analytics"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void ineligibleStudentCannotApply() throws Exception {
        String admin = adminToken();
        long jobId = createOpenJob(admin, "9.0");
        String student = registerStudent(uniqueEmail(), "6.0");

        postJson("/api/applications", student, "{\"jobId\":" + jobId + "}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("CGPA")));
    }

    @Test
    void endpointsAreProtectedByAuthenticationAndRole() throws Exception {
        String student = registerStudent(uniqueEmail(), null);
        String admin = adminToken();

        send(get("/api/student/profile"), null).andExpect(status().isUnauthorized());
        send(get("/api/student/profile"), "not.a.valid.token").andExpect(status().isUnauthorized());
        send(get("/api/admin/analytics"), student).andExpect(status().isForbidden());
        send(get("/api/student/profile"), admin).andExpect(status().isForbidden());
    }

    @Test
    void registrationAndLoginValidation() throws Exception {
        String email = uniqueEmail();
        registerStudent(email, null);

        postJson("/api/auth/register", null,
                "{\"fullName\":\"Again\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
                .andExpect(status().isConflict());
        postJson("/api/auth/register", null, "{\"fullName\":\"\",\"email\":\"bad\",\"password\":\"x\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        postJson("/api/auth/login", null, "{\"email\":\"%s\",\"password\":\"Wrong123\"}".formatted(email))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledStudentIsLockedOutImmediately() throws Exception {
        String email = uniqueEmail();
        String body = postJson("/api/auth/register", null,
                "{\"fullName\":\"Lock Me\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long studentId = ((Number) JsonPath.read(body, "$.id")).longValue();
        String token = loginToken(email, PASSWORD);

        send(get("/api/student/profile"), token).andExpect(status().isOk());
        patchJson("/api/admin/students/" + studentId + "/enabled", adminToken(), "{\"enabled\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        send(get("/api/student/profile"), token).andExpect(status().isUnauthorized());
        postJson("/api/auth/login", null, "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotReadAnotherStudentsApplication() throws Exception {
        String admin = adminToken();
        long jobId = createOpenJob(admin, "0");
        String owner = registerStudent(uniqueEmail(), "8.0");
        String other = registerStudent(uniqueEmail(), "8.0");
        long applicationId = idOf(postJson("/api/applications", owner, "{\"jobId\":" + jobId + "}")
                .andExpect(status().isCreated()));

        send(get("/api/applications/" + applicationId), owner).andExpect(status().isOk());
        send(get("/api/applications/" + applicationId), other).andExpect(status().isNotFound());
    }

    @Test
    void jobWithApplicationsCannotBeDeleted() throws Exception {
        String admin = adminToken();
        long jobId = createOpenJob(admin, "0");
        String student = registerStudent(uniqueEmail(), "8.0");
        postJson("/api/applications", student, "{\"jobId\":" + jobId + "}").andExpect(status().isCreated());

        send(delete("/api/admin/jobs/" + jobId), admin).andExpect(status().isUnprocessableEntity());
    }
}
