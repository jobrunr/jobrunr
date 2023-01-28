package org.jobrunr.server.zookeeper;

import org.jobrunr.server.zookeeper.tasks.ZooKeeperTaskInfo;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Instant;

public class ThreadIdleTaskInfo implements ZooKeeperTaskInfo {

    private final BackgroundJobServerStatus backgroundJobServerStatus;
    private final Instant runStartTime;

    public ThreadIdleTaskInfo(BackgroundJobServerStatus backgroundJobServerStatus) {
        this.backgroundJobServerStatus = backgroundJobServerStatus;
        this.runStartTime = Instant.now();
    }

    @Override
    public BackgroundJobServerStatus getBackgroundJobServerStatus() {
        return backgroundJobServerStatus;
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
