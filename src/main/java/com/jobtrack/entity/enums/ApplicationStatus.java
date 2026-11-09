package com.jobtrack.entity.enums;

/**
 * Application lifecycle. The allowed transitions live here so the rule
 * is defined in one place and is easy to unit test.
 */
public enum ApplicationStatus {
    APPLIED, UNDER_REVIEW, ONLINE_ASSESSMENT, INTERVIEW, SELECTED, REJECTED;

    public boolean canTransitionTo(ApplicationStatus next) {
        return switch (this) {
            case APPLIED -> next == UNDER_REVIEW || next == REJECTED;
            case UNDER_REVIEW -> next == ONLINE_ASSESSMENT || next == INTERVIEW || next == REJECTED;
            case ONLINE_ASSESSMENT -> next == INTERVIEW || next == REJECTED;
            case INTERVIEW -> next == SELECTED || next == REJECTED;
            case SELECTED, REJECTED -> false;
        };
    }

    public boolean isTerminal() {
        return this == SELECTED || this == REJECTED;
    }
}
