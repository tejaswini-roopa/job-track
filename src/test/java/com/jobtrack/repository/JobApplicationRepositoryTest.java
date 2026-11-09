package com.jobtrack.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.jobtrack.entity.Company;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.JobApplication;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.entity.User;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.repository.projection.StatusCount;

@DataJpaTest
class JobApplicationRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JobApplicationRepository applicationRepository;

    private StudentProfile student;
    private Job job;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("student@test.com");
        user.setPasswordHash("hash");
        user.setFullName("Test Student");
        user.setRole(Role.STUDENT);
        em.persist(user);

        student = new StudentProfile();
        student.setUser(user);
        student.setCgpa(new BigDecimal("8.50"));
        em.persist(student);

        Company company = new Company();
        company.setName("Acme");
        em.persist(company);

        job = new Job();
        job.setCompany(company);
        job.setTitle("Java Developer");
        job.setDescription("Build backend services");
        em.persist(job);

        em.flush();
    }

    private JobApplication newApplication() {
        JobApplication app = new JobApplication();
        app.setStudent(student);
        app.setJob(job);
        return app;
    }

    @Test
    void savesApplicationWithDefaultStatusAndTimestamps() {
        JobApplication saved = applicationRepository.saveAndFlush(newApplication());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(saved.getAppliedAt()).isNotNull();
        assertThat(applicationRepository.existsByStudentIdAndJobId(student.getId(), job.getId())).isTrue();
    }

    @Test
    void duplicateApplicationViolatesUniqueConstraint() {
        applicationRepository.saveAndFlush(newApplication());

        assertThatThrownBy(() -> applicationRepository.saveAndFlush(newApplication()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void countsApplicationsGroupedByStatus() {
        applicationRepository.saveAndFlush(newApplication());

        List<StatusCount> counts = applicationRepository.countByStatusForStudent(student.getId());

        assertThat(counts).hasSize(1);
        assertThat(counts.get(0).getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(counts.get(0).getTotal()).isEqualTo(1L);
    }
}
