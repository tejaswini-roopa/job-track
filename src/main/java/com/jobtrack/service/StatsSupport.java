package com.jobtrack.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.jobtrack.dto.response.MonthlyTrendPoint;
import com.jobtrack.entity.enums.ApplicationStatus;
import com.jobtrack.repository.projection.MonthlyCount;
import com.jobtrack.repository.projection.StatusCount;

/** Shared helpers for the dashboard and analytics services (avoids duplicate code). */
final class StatsSupport {

    private StatsSupport() {
    }

    /** Every status is present, with 0 when there are no rows, so charts never miss a slice. */
    static Map<ApplicationStatus, Long> toBreakdown(List<StatusCount> counts) {
        Map<ApplicationStatus, Long> map = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            map.put(s, 0L);
        }
        counts.forEach(c -> map.put(c.getStatus(), c.getTotal()));
        return map;
    }

    static List<MonthlyTrendPoint> toTrend(List<MonthlyCount> counts) {
        return counts.stream()
                .map(c -> new MonthlyTrendPoint(
                        String.format("%d-%02d", c.getPeriodYear(), c.getPeriodMonth()), c.getTotal()))
                .toList();
    }

    static double percentage(long part, long whole) {
        if (whole == 0) {
            return 0.0;
        }
        return Math.round(part * 10000.0 / whole) / 100.0;
    }
}
