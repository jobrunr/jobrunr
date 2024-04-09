package org.jobrunr.server.configuration;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.threadpool.JobRunrExecutor;

import java.util.function.Function;

import static org.jobrunr.utils.VersionNumber.JAVA_VERSION;

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
        if (!threadType.isSupported(JAVA_VERSION)) {
            throw new UnsupportedOperationException(threadType + " is not supported on " + JAVA_VERSION + " (p.s. please make sure your Java Version can be parsed by class VersionNumber).");
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
