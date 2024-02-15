package org.jobrunr.server.configuration;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.PlatformThreadPoolJobRunrExecutor;

public class FixedSizeBackgroundJobServerWorkerPolicy implements BackgroundJobServerWorkerPolicy {

    private final int workerCount;

    public FixedSizeBackgroundJobServerWorkerPolicy(int workerCount) {
        this.workerCount = workerCount;
    }

    @Override
    public WorkDistributionStrategy toWorkDistributionStrategy(BackgroundJobServer backgroundJobServer) {
        return new BasicWorkDistributionStrategy(backgroundJobServer, workerCount);
    }

    @Override
    public JobRunrExecutor toJobRunrExecutor() {
        return new PlatformThreadPoolJobRunrExecutor(workerCount, "backgroundjob-zookeeper-pool");
    }
}
