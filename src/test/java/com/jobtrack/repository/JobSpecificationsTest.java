package com.jobtrack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.jobtrack.dto.request.JobSearchCriteria;
import com.jobtrack.entity.Company;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.enums.JobStatus;
import com.jobtrack.entity.enums.JobType;

@DataJpaTest
class JobSpecificationsTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        Company acme = company("Acme");
        Company globex = company("Globex");
        job(acme, "Java Developer", "Java, Spring Boot", "7.00", JobStatus.OPEN, LocalDate.now().plusDays(10));
        job(globex, "Data Analyst", "SQL, Python", "8.00", JobStatus.OPEN, LocalDate.now().plusDays(10));
        job(acme, "Expired Role", "Java", "0.00", JobStatus.OPEN, LocalDate.now().minusDays(1));
        job(acme, "Closed Role", "Java", "0.00", JobStatus.CLOSED, LocalDate.now().plusDays(10));
        em.flush();
    }

    private Company company(String name) {
        Company c = new Company();
        c.setName(name);
        return em.persist(c);
    }

    private void job(Company c, String title, String skills, String minCgpa, JobStatus status, LocalDate deadline) {
        Job j = new Job();
        j.setCompany(c);
        j.setTitle(title);
        j.setDescription("desc");
        j.setLocation("Pune");
        j.setJobType(JobType.FULL_TIME);
        j.setSkillsRequired(skills);
        j.setMinCgpa(new BigDecimal(minCgpa));
        j.setStatus(status);
        j.setApplicationDeadline(deadline);
        em.persist(j);
    }

    private java.util.List<String> titles(JobSearchCriteria criteria, boolean openOnly, BigDecimal cgpa) {
        return jobRepository.findAll(JobSpecifications.build(criteria, openOnly, cgpa),
                        PageRequest.of(0, 20, Sort.by("title")))
                .getContent().stream().map(Job::getTitle).toList();
    }

    @Test
    void studentsOnlySeeOpenJobsBeforeTheirDeadline() {
        assertThat(titles(JobSearchCriteria.empty(), true, null))
                .containsExactlyInAnyOrder("Java Developer", "Data Analyst");
    }

    @Test
    void adminSeesEveryJob() {
        assertThat(titles(JobSearchCriteria.empty(), false, null)).hasSize(4);
    }

    @Test
    void filtersByCompanyName() {
        var criteria = new JobSearchCriteria("glob", null, null, null, null, null);
        assertThat(titles(criteria, true, null)).containsExactly("Data Analyst");
    }

    @Test
    void anySkillInTheListMatches() {
        var one = new JobSearchCriteria(null, null, null, "spring", null, null);
        var either = new JobSearchCriteria(null, null, null, "sql, spring", null, null);

        assertThat(titles(one, true, null)).containsExactly("Java Developer");
        assertThat(titles(either, true, null)).containsExactlyInAnyOrder("Java Developer", "Data Analyst");
    }

    @Test
    void eligibleOnlyHidesJobsAboveTheStudentsCgpa() {
        var criteria = new JobSearchCriteria(null, null, null, null, null, true);

        assertThat(titles(criteria, true, new BigDecimal("7.50"))).containsExactly("Java Developer");
        assertThat(titles(criteria, true, new BigDecimal("9.00")))
                .containsExactlyInAnyOrder("Java Developer", "Data Analyst");
    }

    @Test
    void roleFilterIsCaseInsensitive() {
        var criteria = new JobSearchCriteria(null, "DATA", null, null, null, null);
        assertThat(titles(criteria, true, null)).containsExactly("Data Analyst");
    }
}
