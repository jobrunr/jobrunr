package org.jobrunr.server.configuration;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.strategy.WorkDistributionStrategy;

public interface BackgroundJobServerWorkerPolicy {

    WorkDistributionStrategy toWorkDistributionStrategy(BackgroundJobServer backgroundJobServer);
}
