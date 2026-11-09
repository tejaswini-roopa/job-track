package com.jobtrack.repository.projection;

public interface MonthlyCount {
    Integer getPeriodYear();
    Integer getPeriodMonth();
    Long getTotal();
}
