package org.jobrunr.jobs.states;

import java.util.function.Predicate;

public enum StateName {

    /** Used for Carbon Aware Jobs **/
    AWAITING,
    SCHEDULED,
    ENQUEUED,
    PROCESSING,
    FAILED,
    SUCCEEDED,
    DELETED;

    public static final Predicate<JobState> FAILED_STATES = FailedState.class::isInstance;

    public static StateName[] getStateNames(StateName... stateNames) {
        if (stateNames.length < 1) {
            return StateName.values();
        }
        return stateNames;
    }
}
