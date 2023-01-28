package org.jobrunr.server.zookeeper;

import org.jobrunr.server.zookeeper.tasks.ZooKeeperTaskInfo;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

public class ZooKeeperRunTaskInfo implements ZooKeeperTaskInfo, AutoCloseable {

    private final ZooKeeperStatistics zooKeeperStatistics;
    private final BackgroundJobServerStatus backgroundJobServerStatus;
    private final long runIndex;
    private final Instant runStartTime;
    private boolean runSucceeded;

    public ZooKeeperRunTaskInfo(ZooKeeperStatistics zooKeeperStatistics, BackgroundJobServerStatus backgroundJobServerStatus, long runIndex) {
        this.zooKeeperStatistics = zooKeeperStatistics;
        this.backgroundJobServerStatus = backgroundJobServerStatus;
        this.runIndex = runIndex;
        this.runStartTime = Instant.now();
        this.runSucceeded = false;
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

    public void markRunAsSucceeded() {
        this.runSucceeded = true;
    }

    @Override
    public void close() {
        zooKeeperStatistics.logRun(runIndex, runSucceeded, backgroundJobServerStatus.getPollIntervalInSeconds(), runStartTime, Instant.now());
    }
}
