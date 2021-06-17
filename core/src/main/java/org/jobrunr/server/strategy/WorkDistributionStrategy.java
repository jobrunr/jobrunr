package org.jobrunr.server.strategy;

import org.jobrunr.storage.PageRequest;

public interface WorkDistributionStrategy {

    int getWorkerCount();

    boolean canOnboardNewWork();

    PageRequest getWorkPageRequest();
}
