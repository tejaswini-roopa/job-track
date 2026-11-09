package com.jobtrack.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.jobtrack.entity.Job;
import com.jobtrack.entity.enums.JobStatus;

/**
 * JpaSpecificationExecutor enables dynamic search/filter queries built in
 * {@link JobSpecifications}.
 */
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    boolean existsByCompanyId(Long companyId);

    long countByStatus(JobStatus status);

    /** Overridden only to attach an EntityGraph so company is fetched in the same query. */
    @Override
    @EntityGraph(attributePaths = "company")
    Page<Job> findAll(Specification<Job> spec, Pageable pageable);
}
