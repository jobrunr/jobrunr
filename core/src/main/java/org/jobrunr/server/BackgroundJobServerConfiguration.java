package org.jobrunr.server;

import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.DefaultBackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.FixedSizeBackgroundJobServerWorkerPolicy;

public class BackgroundJobServerConfiguration {

    int pollIntervalInSeconds = 15;
    BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy = new DefaultBackgroundJobServerWorkerPolicy();

    BackgroundJobServerConfiguration() {

    }

    public static BackgroundJobServerConfiguration usingStandardConfiguration() {
        return new BackgroundJobServerConfiguration();
    }

    public BackgroundJobServerConfiguration andPollIntervalInSeconds(int pollIntervalInSeconds) {
        this.pollIntervalInSeconds = pollIntervalInSeconds;
        return this;
    }

    public BackgroundJobServerConfiguration andWorkerCount(int workerCount) {
        this.backgroundJobServerWorkerPolicy = new FixedSizeBackgroundJobServerWorkerPolicy(workerCount);
        return this;
    }

    public BackgroundJobServerConfiguration andWorkerCountPolicy(BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy) {
        this.backgroundJobServerWorkerPolicy = backgroundJobServerWorkerPolicy;
        return this;
    }

}
