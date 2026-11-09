package com.jobtrack.dto.request;

import com.jobtrack.entity.enums.JobType;

/** All fields optional. {@code skills} is comma separated; any match counts. */
public record JobSearchCriteria(
        String company,
        String role,
        String location,
        String skills,
        JobType jobType,
        Boolean eligibleOnly) {

    public static JobSearchCriteria empty() {
        return new JobSearchCriteria(null, null, null, null, null, null);
    }
}
