package org.jobrunr.server.configuration;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.threadpool.JobRunrExecutor;

import java.util.function.Function;

import static org.jobrunr.utils.VersionNumber.isOlderOrEqualTo;

public class DefaultBackgroundJobServerWorkerPolicy implements BackgroundJobServerWorkerPolicy {

    private final int workerCount;
    private final Function<Integer, JobRunrExecutor> jobRunrExecutorFunction;

    public DefaultBackgroundJobServerWorkerPolicy() {
        this(BackgroundJobServerThreadType.getDefaultThreadType());
    }

    public DefaultBackgroundJobServerWorkerPolicy(BackgroundJobServerThreadType threadType) {
        this(threadType.getDefaultWorkerCount(), threadType);
    }

    public DefaultBackgroundJobServerWorkerPolicy(int workerCount) {
        this(workerCount, BackgroundJobServerThreadType.getDefaultThreadType());
    }

    public DefaultBackgroundJobServerWorkerPolicy(int workerCount, BackgroundJobServerThreadType threadType) {
        this(workerCount, threadType.getJobRunrExecutor());
        if (isOlderOrEqualTo(System.getProperty("java.version"), threadType.getMinimumJavaVersion())) {
            throw new UnsupportedOperationException("The required minimum Java version to use " + threadType + " is " + threadType.getMinimumJavaVersion());
        }
    }

    public DefaultBackgroundJobServerWorkerPolicy(int workerCount, Function<Integer, JobRunrExecutor> jobRunrExecutorFunction) {
        this.workerCount = workerCount;
        this.jobRunrExecutorFunction = jobRunrExecutorFunction;
    }

    @Override
    public WorkDistributionStrategy toWorkDistributionStrategy(BackgroundJobServer backgroundJobServer) {
        return new BasicWorkDistributionStrategy(backgroundJobServer, workerCount);
    }

    @Override
    public JobRunrExecutor toJobRunrExecutor() {
        return jobRunrExecutorFunction.apply(workerCount);
    }
}
