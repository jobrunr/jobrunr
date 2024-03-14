package org.jobrunr.utils;

import java.time.Clock;
import java.time.Instant;

/**
 * This is a utility class to provide the current time.
 * It is used to make the CarbonAwareScheduler testable.
 */
public class TimeProvider {
    private final Clock clock;

    public TimeProvider() {
        this.clock = Clock.systemDefaultZone();
    }

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        return Instant.now(clock);
    }
}
