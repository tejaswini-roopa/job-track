package com.jobtrack.entity;

import static com.jobtrack.entity.enums.ApplicationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApplicationStatusTest {

    @Test
    void validTransitionsAreAllowed() {
        assertThat(APPLIED.canTransitionTo(UNDER_REVIEW)).isTrue();
        assertThat(UNDER_REVIEW.canTransitionTo(ONLINE_ASSESSMENT)).isTrue();
        assertThat(INTERVIEW.canTransitionTo(SELECTED)).isTrue();
    }

    @Test
    void invalidTransitionsAreRejected() {
        assertThat(APPLIED.canTransitionTo(SELECTED)).isFalse();
        assertThat(ONLINE_ASSESSMENT.canTransitionTo(APPLIED)).isFalse();
    }

    @Test
    void terminalStatesCannotChange() {
        assertThat(SELECTED.canTransitionTo(REJECTED)).isFalse();
        assertThat(REJECTED.canTransitionTo(APPLIED)).isFalse();
        assertThat(SELECTED.isTerminal()).isTrue();
    }
}
