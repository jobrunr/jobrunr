package org.jobrunr.jobs.states;

public enum StateName {
    AWAITING,
    SCHEDULED,
    ENQUEUED,
    PROCESSING,
    FAILED,
    SUCCEEDED,
    DELETED
}
