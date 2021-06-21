package org.jobrunr.server.configuration;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;

public class DefaultBackgroundJobServerWorkerPolicy implements BackgroundJobServerWorkerPolicy {

    private final int workerCount;

    public DefaultBackgroundJobServerWorkerPolicy() {
        // see https://jobs.zalando.com/en/tech/blog/how-to-set-an-ideal-thread-pool-size
        workerCount = (Runtime.getRuntime().availableProcessors() * 8);
    }

    @Override
    public WorkDistributionStrategy toWorkDistributionStrategy(BackgroundJobServer backgroundJobServer) {
        return new BasicWorkDistributionStrategy(backgroundJobServer, workerCount);
    }
}
