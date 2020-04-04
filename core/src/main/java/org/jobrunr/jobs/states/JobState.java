package org.jobrunr.jobs.states;

import java.time.Instant;

public interface JobState {

    StateName getName();

    Instant getCreatedAt();

}
