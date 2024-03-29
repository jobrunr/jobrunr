package org.jobrunr.server.zookeeper;

import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.zookeeper.tasks.ZooKeeperTaskInfo;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

public class ZooKeeperRunTaskInfo implements ZooKeeperTaskInfo, AutoCloseable {

    private final ZooKeeperStatistics zooKeeperStatistics;
    private final BackgroundJobServerConfiguration backgroundJobServerConfiguration;
    private final long runIndex;
    private final Instant runStartTime;
    private boolean runSucceeded;

    public ZooKeeperRunTaskInfo(ZooKeeperStatistics zooKeeperStatistics, BackgroundJobServerConfiguration backgroundJobServerConfiguration, long runIndex) {
        this.zooKeeperStatistics = zooKeeperStatistics;
        this.backgroundJobServerConfiguration = backgroundJobServerConfiguration;
        this.runIndex = runIndex;
        this.runStartTime = Instant.now();
        this.runSucceeded = false;
    }

    public boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        final Duration durationPollIntervalTimeBox = Duration.ofMillis((long) backgroundJobServerConfiguration.getPollIntervalInSeconds() * 950);
        final Duration durationRunTime = Duration.between(runStartTime, now());
        return durationRunTime.compareTo(durationPollIntervalTimeBox) >= 0;
    }

    public BackgroundJobServerConfiguration getBackgroundJobServerConfiguration() {
        return backgroundJobServerConfiguration;
    }

    public Instant getRunStartTime() {
        return runStartTime;
    }

    public void markRunAsSucceeded() {
        this.runSucceeded = true;
    }

    @Override
    public void close() {
        zooKeeperStatistics.logRun(runIndex, runSucceeded, backgroundJobServerConfiguration.getPollIntervalInSeconds(), runStartTime, Instant.now());
    }
}
