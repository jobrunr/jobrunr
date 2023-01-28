package org.jobrunr.server.dashboard;

import java.time.Instant;

public class PollIntervalInSecondsTimeBoxIsTooSmallNotification implements DashboardNotification {

    private final long runIndex;
    private final Integer pollIntervalInSeconds;
    private final Instant runStartTime;
    private final Integer actualDurationInSeconds;

    public PollIntervalInSecondsTimeBoxIsTooSmallNotification(long runIndex, Integer pollIntervalInSeconds, Instant runStartTime, Integer actualDurationInSeconds) {
        this.runIndex = runIndex;
        this.pollIntervalInSeconds = pollIntervalInSeconds;
        this.runStartTime = runStartTime;
        this.actualDurationInSeconds = actualDurationInSeconds;
    }

    public long getRunIndex() {
        return runIndex;
    }

    public Instant getRunStartTime() {
        return runStartTime;
    }

    public Integer getPollIntervalInSeconds() {
        return pollIntervalInSeconds;
    }

    public Integer getActualDurationInSeconds() {
        return actualDurationInSeconds;
    }
}
