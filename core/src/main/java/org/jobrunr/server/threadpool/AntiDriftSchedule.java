package org.jobrunr.server.threadpool;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ZERO;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

final class AntiDriftSchedule implements Delayed {
    private final Duration duration;
    private final Runnable runnable;
    private final Instant scheduledAt;

    AntiDriftSchedule(
      final Runnable runnable,
      final Duration initialDelay,
      final Duration duration) {
        super();
        this.runnable = runnable;
        this.duration = duration;
        this.scheduledAt = now().plus(initialDelay);
    }

    AntiDriftSchedule getNextSchedule() {
        return new AntiDriftSchedule(runnable, ZERO, duration);
    }

    Runnable getRunnable() {
        return runnable;
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        final Instant now = now();
        final long nanos = NANOS.between(now, scheduledAt);
        return unit.convert(nanos, NANOSECONDS);
    }

    @Override
    public int compareTo(final Delayed o) {
        return Long.compare(getDelay(NANOSECONDS), o.getDelay(NANOSECONDS));
    }
}
