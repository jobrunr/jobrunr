package org.jobrunr.server.threadpool;

import java.time.Duration;
import java.time.Instant;

class AntiDriftSchedule {
    private final Duration initialDelay;
    private final Duration duration;
    private final Runnable runnable;
    private final Instant firstScheduledAt;
    private long scheduleCount;
    private Instant scheduledAt;

    public AntiDriftSchedule(Runnable runnable, Duration initialDelay, Duration duration) {
        this.runnable = runnable;
        this.initialDelay = initialDelay;
        this.duration = duration;
        this.scheduleCount = 0;
        this.firstScheduledAt = Instant.now().plus(initialDelay);
        this.scheduledAt = firstScheduledAt;
    }

    public Instant getScheduledAt() {
        return this.scheduledAt;
    }

    public Instant getNextSchedule() {
        this.scheduledAt = firstScheduledAt.plus(duration.multipliedBy(scheduleCount));
        scheduleCount++;
        return scheduledAt;
    }

    Runnable getRunnable() {
        return runnable;
    }
}
