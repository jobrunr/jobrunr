package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.server.BackgroundJobServerConfigurationReader;

import java.time.Instant;

public interface ZooKeeperTaskInfo {

    BackgroundJobServerConfigurationReader getBackgroundJobServerConfiguration();

    boolean pollIntervalInSecondsTimeBoxIsAboutToPass();

    Instant getRunStartTime();
}
