package org.jobrunr.server.configuration;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.ScheduledThreadPoolJobRunrExecutor;
import org.jobrunr.server.threadpool.VirtualThreadPoolJobRunrExecutor;
import org.jobrunr.utils.VersionNumber;

public class DefaultBackgroundJobServerWorkerPolicy implements BackgroundJobServerWorkerPolicy {

    private final JobRunrExecutor jobRunrExecutor;

    public DefaultBackgroundJobServerWorkerPolicy() {
        jobRunrExecutor = VersionNumber.isNewerOrEqualTo(System.getProperty("java.version"), "21")
                ? new VirtualThreadPoolJobRunrExecutor(getDefaultWorkerCount() * 2)
                : new ScheduledThreadPoolJobRunrExecutor(getDefaultWorkerCount());
    }

    public DefaultBackgroundJobServerWorkerPolicy(JobRunrExecutor jobRunrExecutor) {
        this.jobRunrExecutor = jobRunrExecutor;
    }

    @Override
    public WorkDistributionStrategy toWorkDistributionStrategy(BackgroundJobServer backgroundJobServer) {
        return new BasicWorkDistributionStrategy(backgroundJobServer, jobRunrExecutor.getWorkerCount());
    }

    @Override
    public JobRunrExecutor toJobRunrExecutor() {
        return jobRunrExecutor;
    }

    protected int getDefaultWorkerCount() {
        // see https://jobs.zalando.com/en/tech/blog/how-to-set-an-ideal-thread-pool-size
        return (Runtime.getRuntime().availableProcessors() * 8);
    }
}
