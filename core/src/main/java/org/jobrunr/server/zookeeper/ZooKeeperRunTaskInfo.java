package org.jobrunr.server.zookeeper;

import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.server.zookeeper.tasks.ZooKeeperTaskInfo;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

public class ZooKeeperRunTaskInfo implements ZooKeeperTaskInfo, AutoCloseable {

    private final ZooKeeperStatistics zooKeeperStatistics;
    private final BackgroundJobServerConfigurationReader configuration;
    private final long runIndex;
    private final Instant runStartTime;
    private boolean runSucceeded;

    public ZooKeeperRunTaskInfo(ZooKeeperStatistics zooKeeperStatistics, BackgroundJobServerConfigurationReader backgroundJobServerConfiguration, long runIndex) {
        this.zooKeeperStatistics = zooKeeperStatistics;
        this.configuration = backgroundJobServerConfiguration;
        this.runIndex = runIndex;
        this.runStartTime = Instant.now();
        this.runSucceeded = false;
    }

    @Override
    public boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        final Duration durationPollIntervalTimeBox = configuration.getPollInterval().multipliedBy(95).dividedBy(100);
        final Duration durationRunTime = Duration.between(runStartTime, now());
        return durationRunTime.compareTo(durationPollIntervalTimeBox) >= 0;
    }

    @Override
    public BackgroundJobServerConfigurationReader getBackgroundJobServerConfiguration() {
        return configuration;
    }

    @Override
    public Instant getRunStartTime() {
        return runStartTime;
    }

    public void markRunAsSucceeded() {
        this.runSucceeded = true;
    }

    @Override
    public void close() {
        zooKeeperStatistics.logRun(runIndex, runSucceeded, configuration.getPollInterval(), runStartTime, Instant.now());
    }
}
