package com.jobtrack.exception;

/** A valid request that breaks a business rule (closed job, bad transition...). Maps to HTTP 422. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
