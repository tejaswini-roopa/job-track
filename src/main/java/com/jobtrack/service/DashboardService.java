package com.jobtrack.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtrack.dto.response.DashboardResponse;
import com.jobtrack.dto.response.InterviewResponse;
import com.jobtrack.dto.response.MonthlyTrendPoint;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.mapper.EntityMapper;
import com.jobtrack.repository.InterviewRepository;
import com.jobtrack.repository.JobApplicationRepository;
import com.jobtrack.repository.StudentProfileRepository;

@Service
public class DashboardService {

    private static final int UPCOMING_LIMIT = 5;

    private final JobApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final StudentProfileRepository profileRepository;

    public DashboardService(JobApplicationRepository applicationRepository,
                            InterviewRepository interviewRepository,
                            StudentProfileRepository profileRepository) {
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getStudentDashboard(Long userId) {
        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for this user"));
        Long studentId = profile.getId();

        Map<ApplicationStatus, Long> breakdown =
                StatsSupport.toBreakdown(applicationRepository.countByStatusForStudent(studentId));
        long total = breakdown.values().stream().mapToLong(Long::longValue).sum();

        List<InterviewResponse> upcoming = interviewRepository
                .findByApplicationStudentIdAndScheduledAtAfterOrderByScheduledAtAsc(studentId, LocalDateTime.now())
                .stream()
                .limit(UPCOMING_LIMIT)
                .map(EntityMapper::toInterviewResponse)
                .toList();

        List<MonthlyTrendPoint> trend =
                StatsSupport.toTrend(applicationRepository.countMonthlyForStudent(studentId));

        return new DashboardResponse(total,
                breakdown.get(ApplicationStatus.UNDER_REVIEW),
                breakdown.get(ApplicationStatus.ONLINE_ASSESSMENT),
                breakdown.get(ApplicationStatus.INTERVIEW),
                breakdown.get(ApplicationStatus.SELECTED),
                breakdown.get(ApplicationStatus.REJECTED),
                breakdown, upcoming, trend);
    }
}
