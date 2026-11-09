package com.jobtrack.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtrack.entity.SavedJob;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    boolean existsByStudentIdAndJobId(Long studentId, Long jobId);

    Optional<SavedJob> findByStudentIdAndJobId(Long studentId, Long jobId);

    @EntityGraph(attributePaths = {"job", "job.company"})
    Page<SavedJob> findByStudentId(Long studentId, Pageable pageable);
}
