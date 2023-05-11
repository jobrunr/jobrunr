package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.server.BackgroundJobServerConfiguration;

import java.time.Instant;

public interface ZooKeeperTaskInfo {

    BackgroundJobServerConfiguration getBackgroundJobServerConfiguration();

    boolean pollIntervalInSecondsTimeBoxIsAboutToPass();

    Instant getRunStartTime();
}
