package com.jobtrack.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtrack.dto.response.AnalyticsResponse;
import com.jobtrack.dto.response.CompanyStat;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.entity.enums.JobStatus;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.repository.JobApplicationRepository;
import com.jobtrack.repository.JobRepository;
import com.jobtrack.repository.UserRepository;

@Service
public class AnalyticsService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public AnalyticsService(JobApplicationRepository applicationRepository,
                            JobRepository jobRepository,
                            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getOverview() {
        Map<ApplicationStatus, Long> breakdown =
                StatsSupport.toBreakdown(applicationRepository.countByStatusOverall());
        long total = breakdown.values().stream().mapToLong(Long::longValue).sum();
        long selected = breakdown.get(ApplicationStatus.SELECTED);
        long rejected = breakdown.get(ApplicationStatus.REJECTED);
        long decided = selected + rejected;

        List<CompanyStat> companies = applicationRepository.countByCompany().stream()
                .map(c -> new CompanyStat(c.getCompanyName(), c.getTotal()))
                .toList();

        return new AnalyticsResponse(total,
                userRepository.countByRole(Role.STUDENT),
                jobRepository.countByStatus(JobStatus.OPEN),
                selected, rejected,
                StatsSupport.percentage(selected, total),
                StatsSupport.percentage(selected, decided),
                StatsSupport.percentage(rejected, decided),
                breakdown, companies,
                StatsSupport.toTrend(applicationRepository.countMonthlyOverall()));
    }
}
