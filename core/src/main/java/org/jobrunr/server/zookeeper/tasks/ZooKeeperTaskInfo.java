package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Instant;

public interface ZooKeeperTaskInfo {

    BackgroundJobServerStatus getBackgroundJobServerStatus();

    boolean pollIntervalInSecondsTimeBoxIsAboutToPass();

    Instant getRunStartTime();
}
