package org.jobrunr.server.strategy;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicWorkDistributionStrategy implements WorkDistributionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicWorkDistributionStrategy.class);

    private final BackgroundJobServer backgroundJobServer;
    private final BackgroundJobServerStatus backgroundJobServerStatus;

    public BasicWorkDistributionStrategy(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.backgroundJobServerStatus = backgroundJobServer.getServerStatus();
    }

    @Override
    public boolean canOnboardNewWork() {
        final int workQueueSize = backgroundJobServer.getWorkQueueSize();
        final int workQueueSizeBuffer = backgroundJobServerStatus.getWorkerPoolSize();
        return workQueueSize < workQueueSizeBuffer;
    }

    @Override
    public PageRequest getWorkPageRequest() {
        final int workQueueSize = backgroundJobServer.getWorkQueueSize();
        final int workerPoolSize = backgroundJobServerStatus.getWorkerPoolSize();

        final long offset = 0;
        final int limit = workerPoolSize - workQueueSize;
        return PageRequest.of(offset, limit);
    }
}
