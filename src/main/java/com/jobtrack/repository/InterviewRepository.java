package com.jobtrack.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtrack.entity.Interview;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    /** Upcoming interviews for one student (path: interview.application.student.id). */
    @EntityGraph(attributePaths = {"application", "application.job", "application.job.company"})
    List<Interview> findByApplicationStudentIdAndScheduledAtAfterOrderByScheduledAtAsc(
            Long studentId, LocalDateTime after);

    List<Interview> findByApplicationIdOrderByScheduledAtAsc(Long applicationId);
}
