package org.jobrunr.jobs.states;

import java.util.function.Predicate;

public enum StateName {

    AWAITING,
    SCHEDULED,
    ENQUEUED,
    PROCESSING,
    FAILED,
    SUCCEEDED,
    DELETED;

    public static final Predicate<JobState> FAILED_STATES = state -> state instanceof FailedState;
}
