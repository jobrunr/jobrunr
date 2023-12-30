package org.jobrunr.server.configuration;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.ScheduledThreadPoolJobRunrExecutor;
import org.jobrunr.server.threadpool.VirtualThreadPoolJobRunrExecutor;

import java.util.function.Function;

import static org.jobrunr.utils.VersionNumber.isNewerOrEqualTo;
import static org.jobrunr.utils.VersionNumber.isOlderOrEqualTo;

public class DefaultBackgroundJobServerWorkerPolicy implements BackgroundJobServerWorkerPolicy {

    public enum ThreadType {
        PlatformThreads("8") {
            @Override
            public Function<Integer, JobRunrExecutor> getJobRunrExecutor() {
                return ScheduledThreadPoolJobRunrExecutor::new;
            }

            @Override
            int getDefaultWorkerCount() {
                return DefaultBackgroundJobServerWorkerPolicy.getDefaultWorkerCount();
            }
        },
        VirtualThreads("21") {
            @Override
            public Function<Integer, JobRunrExecutor> getJobRunrExecutor() {
                return VirtualThreadPoolJobRunrExecutor::new;
            }

            @Override
            int getDefaultWorkerCount() {
                return DefaultBackgroundJobServerWorkerPolicy.getDefaultWorkerCount() * 2;
            }
        };

        private final String minimumJavaVersion;

        ThreadType(String minimumJavaVersion) {
            this.minimumJavaVersion = minimumJavaVersion;
        }

        abstract Function<Integer, JobRunrExecutor> getJobRunrExecutor();

        abstract int getDefaultWorkerCount();

        public String getMinimumJavaVersion() {
            return minimumJavaVersion;
        }
    }

    private final int workerCount;
    private final Function<Integer, JobRunrExecutor> jobRunrExecutorFunction;

    public DefaultBackgroundJobServerWorkerPolicy() {
        this(isNewerOrEqualTo(System.getProperty("java.version"), "21") ? ThreadType.VirtualThreads : ThreadType.PlatformThreads);
    }

    public DefaultBackgroundJobServerWorkerPolicy(ThreadType threadType) {
        this(threadType.getDefaultWorkerCount(), threadType);
    }

    public DefaultBackgroundJobServerWorkerPolicy(int workerCount, ThreadType threadType) {
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

    protected static int getDefaultWorkerCount() {
        // see https://jobs.zalando.com/en/tech/blog/how-to-set-an-ideal-thread-pool-size
        return (Runtime.getRuntime().availableProcessors() * 8);
    }
}
