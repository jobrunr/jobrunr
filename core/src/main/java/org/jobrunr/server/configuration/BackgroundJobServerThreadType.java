package org.jobrunr.server.configuration;

import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.ScheduledThreadPoolJobRunrExecutor;
import org.jobrunr.server.threadpool.VirtualThreadPoolJobRunrExecutor;

import java.util.function.Function;

import static org.jobrunr.utils.VersionNumber.isNewerOrEqualTo;

public enum BackgroundJobServerThreadType {
    PlatformThreads("1.8") {
        @Override
        public Function<Integer, JobRunrExecutor> getJobRunrExecutor() {
            return ScheduledThreadPoolJobRunrExecutor::new;
        }

    },
    VirtualThreads("21") {
        @Override
        public Function<Integer, JobRunrExecutor> getJobRunrExecutor() {
            return VirtualThreadPoolJobRunrExecutor::new;
        }

        @Override
        public int getDefaultWorkerCount() {
            return super.getDefaultWorkerCount() * 2;
        }
    };

    private final String minimumJavaVersion;

    BackgroundJobServerThreadType(String minimumJavaVersion) {
        this.minimumJavaVersion = minimumJavaVersion;
    }

    abstract Function<Integer, JobRunrExecutor> getJobRunrExecutor();

    public int getDefaultWorkerCount() {
        // see https://jobs.zalando.com/en/tech/blog/how-to-set-an-ideal-thread-pool-size
        return (Runtime.getRuntime().availableProcessors() * 8);
    }

    public String getMinimumJavaVersion() {
        return minimumJavaVersion;
    }

    public static BackgroundJobServerThreadType getDefaultThreadType() {
        return isNewerOrEqualTo(System.getProperty("java.version"), "21") ? BackgroundJobServerThreadType.VirtualThreads : BackgroundJobServerThreadType.PlatformThreads;
    }
}
