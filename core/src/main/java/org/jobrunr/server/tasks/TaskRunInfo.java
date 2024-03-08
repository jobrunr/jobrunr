package org.jobrunr.server.tasks;

import org.jobrunr.server.BackgroundJobServerConfigurationReader;

import java.time.Instant;

public abstract class TaskRunInfo {

    private final BackgroundJobServerConfigurationReader backgroundJobServerConfiguration;
    private final Instant runStartTime;

    protected TaskRunInfo(BackgroundJobServerConfigurationReader backgroundJobServerConfiguration) {
        this.backgroundJobServerConfiguration = backgroundJobServerConfiguration;
        this.runStartTime = Instant.now();
    }

    public BackgroundJobServerConfigurationReader getBackgroundJobServerConfiguration() {
        return backgroundJobServerConfiguration;
    }

    public Instant getRunStartTime() {
        return runStartTime;
    }

    public boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        return false;
    }
}