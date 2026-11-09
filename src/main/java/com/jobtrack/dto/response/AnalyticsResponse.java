package com.jobtrack.dto.response;

import java.util.List;
import java.util.Map;

import com.jobtrack.entity.enums.ApplicationStatus;

/**
 * successRate = selected / total applications (%).
 * selectionRate and rejectionRate = share of decided applications (%).
 */
public record AnalyticsResponse(long totalApplications, long totalStudents, long openJobs,
                                long selected, long rejected,
                                double successRate, double selectionRate, double rejectionRate,
                                Map<ApplicationStatus, Long> statusBreakdown,
                                List<CompanyStat> companyStats,
                                List<MonthlyTrendPoint> monthlyTrend) {
}
