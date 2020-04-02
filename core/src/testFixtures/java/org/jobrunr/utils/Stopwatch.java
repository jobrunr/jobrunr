package org.jobrunr.utils;

import java.time.Duration;
import java.time.Instant;

public class Stopwatch implements AutoCloseable {

    private boolean isStarted;
    private Instant started;
    private Instant stopped;

    public Stopwatch start() {
        if (isStarted) {
            throw new IllegalStateException("StopWatch is already running");
        }
        isStarted = true;
        started = Instant.now();
        return this;
    }

    /**
     * Stop elapsed time and make the state of stopwatch stop.
     *
     * @return this instance of StopWatch.
     */
    public Stopwatch stop() {
        if (!isStarted) {
            throw new IllegalStateException("StopWatch is already stopped");
        }
        stopped = Instant.now();
        isStarted = false;
        return this;
    }

    /**
     * Reset elapsed time to zero and make the state of stopwatch stop.
     *
     * @return this instance of StopWatch.
     */
    public Stopwatch reset() {
        started = null;
        stopped = null;
        isStarted = false;
        return this;
    }

    public Duration duration() {
        return Duration.between(started, stopped);
    }

    @Override
    public void close() {
        if (isStarted) {
            stop();
        }
    }
}
