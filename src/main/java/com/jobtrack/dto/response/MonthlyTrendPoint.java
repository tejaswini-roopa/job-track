package com.jobtrack.dto.response;

/** {@code month} is formatted as yyyy-MM, e.g. 2026-11. */
public record MonthlyTrendPoint(String month, long total) {
}
