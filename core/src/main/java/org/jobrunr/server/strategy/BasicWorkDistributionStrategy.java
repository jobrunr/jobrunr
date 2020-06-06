package org.jobrunr.server.strategy;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicWorkDistributionStrategy implements WorkDistributionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicWorkDistributionStrategy.class);

    private final BackgroundJobServerStatus backgroundJobServerStatus;
    private final JobZooKeeper jobZooKeeper;

    public BasicWorkDistributionStrategy(BackgroundJobServer backgroundJobServer, JobZooKeeper jobZooKeeper) {
        this.backgroundJobServerStatus = backgroundJobServer.getServerStatus();
        this.jobZooKeeper = jobZooKeeper;
    }

    @Override
    public boolean canOnboardNewWork() {
        final double workQueueSize = jobZooKeeper.getWorkQueueSize();
        final double workerPoolSize = backgroundJobServerStatus.getWorkerPoolSize();
        final boolean canOnboardWork = (workQueueSize / workerPoolSize) < 0.7;
        LOGGER.info(canOnboardWork ? String.format("Can onboard new work (workQueueSize = %d; workerPoolSize = %d).", (int) workQueueSize, (int) workerPoolSize) : String.format("Can NOT onboard new work (workQueueSize = %d; workerPoolSize = %d).", (int) workQueueSize, (int) workerPoolSize));
        return canOnboardWork;
    }

    @Override
    public PageRequest getWorkPageRequest() {
        final int workQueueSize = jobZooKeeper.getWorkQueueSize();
        final int workerPoolSize = backgroundJobServerStatus.getWorkerPoolSize();

        final long offset = 0;
        final int limit = workerPoolSize - workQueueSize;
        //LOGGER.info(String.format("Can onboard " + limit + " new work (workQueueSize = %f; workerPoolSize = %f).", workQueueSize, workerPoolSize);
        return PageRequest.asc(offset, limit);
    }
}
