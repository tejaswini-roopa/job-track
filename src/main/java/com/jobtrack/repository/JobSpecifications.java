package com.jobtrack.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.jobtrack.dto.request.JobSearchCriteria;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.enums.JobStatus;

import jakarta.persistence.criteria.Predicate;

/** Builds the dynamic WHERE clause for job search from optional filters. */
public final class JobSpecifications {

    private JobSpecifications() {
    }

    /**
     * @param openOnly    true for students: only OPEN jobs whose deadline has not passed
     * @param studentCgpa used only when {@code eligibleOnly} is set; jobs with minCgpa above it are hidden
     */
    public static Specification<Job> build(JobSearchCriteria c, boolean openOnly, BigDecimal studentCgpa) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (openOnly) {
                predicates.add(cb.equal(root.get("status"), JobStatus.OPEN));
                predicates.add(cb.or(
                        cb.isNull(root.get("applicationDeadline")),
                        cb.greaterThanOrEqualTo(root.<LocalDate>get("applicationDeadline"), LocalDate.now())));
            }
            if (hasText(c.company())) {
                predicates.add(cb.like(cb.lower(root.join("company").<String>get("name")), like(c.company())));
            }
            if (hasText(c.role())) {
                predicates.add(cb.like(cb.lower(root.<String>get("title")), like(c.role())));
            }
            if (hasText(c.location())) {
                predicates.add(cb.like(cb.lower(root.<String>get("location")), like(c.location())));
            }
            if (hasText(c.skills())) {
                List<Predicate> skillMatches = Arrays.stream(c.skills().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> cb.like(cb.lower(root.<String>get("skillsRequired")), like(s)))
                        .toList();
                if (!skillMatches.isEmpty()) {
                    predicates.add(cb.or(skillMatches.toArray(new Predicate[0])));
                }
            }
            if (c.jobType() != null) {
                predicates.add(cb.equal(root.get("jobType"), c.jobType()));
            }
            if (Boolean.TRUE.equals(c.eligibleOnly()) && studentCgpa != null) {
                predicates.add(cb.or(
                        cb.isNull(root.get("minCgpa")),
                        cb.lessThanOrEqualTo(root.<BigDecimal>get("minCgpa"), studentCgpa)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String like(String s) {
        return "%" + s.trim().toLowerCase() + "%";
    }
}
