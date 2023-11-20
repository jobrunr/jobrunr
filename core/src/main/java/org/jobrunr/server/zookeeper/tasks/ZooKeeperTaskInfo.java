package org.jobrunr.server.zookeeper.tasks;

import java.time.Instant;

public interface ZooKeeperTaskInfo {

    boolean pollIntervalInSecondsTimeBoxIsAboutToPass();

    Instant getRunStartTime();
}
