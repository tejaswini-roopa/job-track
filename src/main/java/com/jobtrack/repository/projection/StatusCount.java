package com.jobtrack.repository.projection;

import com.jobtrack.entity.enums.ApplicationStatus;

public interface StatusCount {
    ApplicationStatus getStatus();
    Long getTotal();
}
