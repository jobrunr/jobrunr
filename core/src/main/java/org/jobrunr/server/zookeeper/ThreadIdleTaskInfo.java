package org.jobrunr.server.zookeeper;

import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.server.zookeeper.tasks.ZooKeeperTaskInfo;

import java.time.Instant;

public class ThreadIdleTaskInfo implements ZooKeeperTaskInfo {

    private final BackgroundJobServerConfigurationReader configuration;
    private final Instant runStartTime;

    public ThreadIdleTaskInfo(BackgroundJobServerConfigurationReader getBackgroundJobServerConfiguration) {
        this.configuration = getBackgroundJobServerConfiguration;
        this.runStartTime = Instant.now();
    }

    @Override
    public BackgroundJobServerConfigurationReader getBackgroundJobServerConfiguration() {
        return configuration;
    }

    @Override
    public boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        return false;
    }

    @Override
    public Instant getRunStartTime() {
        return runStartTime;
    }

}
