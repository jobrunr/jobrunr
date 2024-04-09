package org.jobrunr.server.strategy;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.navigation.AmountRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

public class BasicWorkDistributionStrategy implements WorkDistributionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicWorkDistributionStrategy.class);

    private final BackgroundJobServer backgroundJobServer;
    private final int workerCount;

    public BasicWorkDistributionStrategy(BackgroundJobServer backgroundJobServer, int workerCount) {
        this.backgroundJobServer = backgroundJobServer;
        this.workerCount = workerCount;
    }

    @Override
    public int getWorkerCount() {
        return workerCount;
    }

    @Override
    public boolean canOnboardNewWork() {
        final double occupiedWorkerCount = getOccupiedWorkerCount();
        final boolean canOnboardWork = (occupiedWorkerCount / workerCount) < 0.7;
        return canOnboardWork;
    }

    @Override
    public AmountRequest getWorkPageRequest() {
        final int occupiedWorkerCount = getOccupiedWorkerCount();

        final int limit = workerCount - occupiedWorkerCount;
        LOGGER.debug("Can onboard {} new work (occupiedWorkerCount = {}; workerCount = {}).", limit, occupiedWorkerCount, workerCount);
        return ascOnUpdatedAt(limit);
    }

    private int getOccupiedWorkerCount() {
        return backgroundJobServer.getJobSteward().getOccupiedWorkerCount();
    }
}
