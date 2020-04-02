package org.jobrunr.jobs.states;

import java.time.Instant;
import java.util.function.Predicate;

public interface JobState {

    Predicate<JobState> FAILED_STATES = state -> state instanceof FailedState;
    Predicate<JobState> SCHEDULED_STATES = state -> state instanceof ScheduledState;

    StateName getName();

    Instant getCreatedAt();

}
