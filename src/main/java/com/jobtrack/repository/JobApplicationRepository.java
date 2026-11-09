package com.jobtrack.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jobtrack.entity.JobApplication;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.repository.projection.CompanyCount;
import com.jobtrack.repository.projection.MonthlyCount;
import com.jobtrack.repository.projection.StatusCount;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByStudentIdAndJobId(Long studentId, Long jobId);

    Optional<JobApplication> findByIdAndStudentId(Long id, Long studentId);

    long countByStudentId(Long studentId);

    /** EntityGraph fetches job and company in the same query, avoiding N+1 selects. */
    @EntityGraph(attributePaths = {"job", "job.company"})
    Page<JobApplication> findByStudentId(Long studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.company"})
    Page<JobApplication> findByStudentIdAndStatus(Long studentId, ApplicationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "job", "job.company"})
    Page<JobApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    @Query("select a.status as status, count(a) as total "
         + "from JobApplication a where a.student.id = :studentId group by a.status")
    List<StatusCount> countByStatusForStudent(@Param("studentId") Long studentId);

    @Query("select a.status as status, count(a) as total from JobApplication a group by a.status")
    List<StatusCount> countByStatusOverall();

    @Query("select a.job.company.name as companyName, count(a) as total "
         + "from JobApplication a group by a.job.company.name order by count(a) desc")
    List<CompanyCount> countByCompany();

    @Query("select year(a.appliedAt) as periodYear, month(a.appliedAt) as periodMonth, count(a) as total "
         + "from JobApplication a where a.student.id = :studentId "
         + "group by year(a.appliedAt), month(a.appliedAt) "
         + "order by year(a.appliedAt), month(a.appliedAt)")
    List<MonthlyCount> countMonthlyForStudent(@Param("studentId") Long studentId);

    @Query("select year(a.appliedAt) as periodYear, month(a.appliedAt) as periodMonth, count(a) as total "
         + "from JobApplication a "
         + "group by year(a.appliedAt), month(a.appliedAt) "
         + "order by year(a.appliedAt), month(a.appliedAt)")
    List<MonthlyCount> countMonthlyOverall();

    boolean existsByJobId(Long jobId);

    @Override
    @EntityGraph(attributePaths = {"student", "student.user", "job", "job.company"})
    Page<JobApplication> findAll(Pageable pageable);
}
