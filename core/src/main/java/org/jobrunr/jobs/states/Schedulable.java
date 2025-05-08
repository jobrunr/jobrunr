package org.jobrunr.jobs.states;

import java.time.Instant;

public interface Schedulable {

    Instant getScheduledAt();
}
