package org.jobrunr.server.configuration;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.threadpool.JobRunrExecutor;

public interface BackgroundJobServerWorkerPolicy {

    WorkDistributionStrategy toWorkDistributionStrategy(BackgroundJobServer backgroundJobServer);

    JobRunrExecutor toJobRunrExecutor();
}
