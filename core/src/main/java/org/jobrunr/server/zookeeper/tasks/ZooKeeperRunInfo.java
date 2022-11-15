package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

public class ZooKeeperRunInfo {

    private final Instant runStartTime;
    private final BackgroundJobServerStatus backgroundJobServerStatus;

    public ZooKeeperRunInfo(BackgroundJobServerStatus backgroundJobServerStatus) {
        this.runStartTime = Instant.now();
        this.backgroundJobServerStatus = backgroundJobServerStatus;
    }

    public boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        final Duration durationPollIntervalTimeBox = Duration.ofMillis((long) backgroundJobServerStatus.getPollIntervalInSeconds() * 950);
        final Duration durationRunTime = Duration.between(runStartTime, now());
        return durationRunTime.compareTo(durationPollIntervalTimeBox) >= 0;
    }

    public BackgroundJobServerStatus getBackgroundJobServerStatus() {
        return backgroundJobServerStatus;
    }

    public Instant getRunStartTime() {
        return runStartTime;
    }
}
