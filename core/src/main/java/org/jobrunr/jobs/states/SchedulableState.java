package org.jobrunr.jobs.states;

import java.time.Instant;

public interface SchedulableState extends JobState {

    Instant getScheduledAt();
}
