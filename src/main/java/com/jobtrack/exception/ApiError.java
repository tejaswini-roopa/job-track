package com.jobtrack.exception;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** The single JSON error shape returned by every failing API call. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(LocalDateTime timestamp, int status, String error, String message,
                       String path, Map<String, String> fieldErrors) {
}
