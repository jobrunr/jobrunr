package org.jobrunr.server.zookeeper;

import org.jobrunr.server.zookeeper.tasks.ZooKeeperTaskInfo;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

public class ZooKeeperRunTaskInfo implements ZooKeeperTaskInfo, AutoCloseable {

    private final ZooKeeperStatistics zooKeeperStatistics;
    private final Duration pollIntervalDuration;
    private final Duration pollIntervalDurationTimeBox;
    private final long runIndex;
    private final Instant runStartTime;
    private boolean runSucceeded;

    public ZooKeeperRunTaskInfo(ZooKeeperStatistics zooKeeperStatistics, Duration pollIntervalDuration, long runIndex) {
        this.zooKeeperStatistics = zooKeeperStatistics;
        this.pollIntervalDuration = pollIntervalDuration;
        this.pollIntervalDurationTimeBox = pollIntervalDuration.multipliedBy(95).dividedBy(100);
        this.runIndex = runIndex;
        this.runStartTime = Instant.now();
        this.runSucceeded = false;
    }

    public boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        final Duration durationRunTime = Duration.between(runStartTime, now());
        return durationRunTime.compareTo(pollIntervalDurationTimeBox) >= 0;
    }

    public Instant getRunStartTime() {
        return runStartTime;
    }

    public void markRunAsSucceeded() {
        this.runSucceeded = true;
    }

    @Override
    public void close() {
        zooKeeperStatistics.logRun(runIndex, runSucceeded, pollIntervalDuration, runStartTime, Instant.now());
    }
}
