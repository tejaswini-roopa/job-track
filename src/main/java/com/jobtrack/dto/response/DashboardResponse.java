package com.jobtrack.dto.response;

import java.util.List;
import java.util.Map;

import com.jobtrack.entity.enums.ApplicationStatus;

public record DashboardResponse(long totalApplications, long underReview, long onlineAssessment,
                                long interview, long selected, long rejected,
                                Map<ApplicationStatus, Long> statusBreakdown,
                                List<InterviewResponse> upcomingInterviews,
                                List<MonthlyTrendPoint> monthlyTrend) {
}
