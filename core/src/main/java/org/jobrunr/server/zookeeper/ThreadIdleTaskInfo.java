package org.jobrunr.server.zookeeper;

import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.zookeeper.tasks.ZooKeeperTaskInfo;

import java.time.Instant;

public class ThreadIdleTaskInfo implements ZooKeeperTaskInfo {

    private final BackgroundJobServerConfiguration getBackgroundJobServerConfiguration;
    private final Instant runStartTime;

    public ThreadIdleTaskInfo(BackgroundJobServerConfiguration getBackgroundJobServerConfiguration) {
        this.getBackgroundJobServerConfiguration = getBackgroundJobServerConfiguration;
        this.runStartTime = Instant.now();
    }

    @Override
    public BackgroundJobServerConfiguration getBackgroundJobServerConfiguration() {
        return getBackgroundJobServerConfiguration;
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
