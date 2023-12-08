package org.jobrunr.server.strategy;

import org.jobrunr.storage.navigation.AmountRequest;

public interface WorkDistributionStrategy {

    int getWorkerCount();

    boolean canOnboardNewWork();

    AmountRequest getWorkPageRequest();
}
