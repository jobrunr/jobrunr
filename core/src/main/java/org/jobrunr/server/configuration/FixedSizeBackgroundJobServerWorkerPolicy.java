package org.jobrunr.server.configuration;

public class FixedSizeBackgroundJobServerWorkerPolicy implements BackgroundJobServerWorkerPolicy {

    private final int workerCount;

    public FixedSizeBackgroundJobServerWorkerPolicy(int workerCount) {
        this.workerCount = workerCount;
    }

    @Override
    public int getWorkerCount() {
        return workerCount;
    }
}
