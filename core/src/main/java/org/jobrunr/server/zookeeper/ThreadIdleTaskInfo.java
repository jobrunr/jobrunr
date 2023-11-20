package org.jobrunr.server.zookeeper;

import org.jobrunr.server.zookeeper.tasks.ZooKeeperTaskInfo;

import java.time.Instant;

public class ThreadIdleTaskInfo implements ZooKeeperTaskInfo {

    private final Instant runStartTime;

    public ThreadIdleTaskInfo() {
        this.runStartTime = Instant.now();
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
