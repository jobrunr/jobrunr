package org.jobrunr.server.configuration;

import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.PlatformThreadPoolJobRunrExecutor;
import org.jobrunr.server.threadpool.VirtualThreadJobRunrExecutor;
import org.jobrunr.utils.VersionNumber;

import java.util.function.Function;

import static org.jobrunr.utils.VersionNumber.JAVA_VERSION;

/**
 * Enum representing the different types of background job server threads.
 */
public enum BackgroundJobServerThreadType {
    PlatformThreads {
        @Override
        public Function<Integer, JobRunrExecutor> getJobRunrExecutor() {
            return PlatformThreadPoolJobRunrExecutor::new;
        }

        @Override
        public boolean isSupported(VersionNumber javaVersion) {
            return true;
        }

    },
    VirtualThreads {
        @Override
        public Function<Integer, JobRunrExecutor> getJobRunrExecutor() {
            return VirtualThreadJobRunrExecutor::new;
        }

        @Override
        public boolean isSupported(VersionNumber javaVersion) {
            return javaVersion.hasMajorVersionHigherOrEqualTo(21);
        }

        @Override
        public int getDefaultWorkerCount() {
            return super.getDefaultWorkerCount() * 2;
        }
    };

    abstract Function<Integer, JobRunrExecutor> getJobRunrExecutor();

    public abstract boolean isSupported(VersionNumber javaVersion);

    public int getDefaultWorkerCount() {
        // see https://jobs.zalando.com/en/tech/blog/how-to-set-an-ideal-thread-pool-size
        return (Runtime.getRuntime().availableProcessors() * 8);
    }

    public static BackgroundJobServerThreadType getDefaultThreadType() {
        return VirtualThreads.isSupported(JAVA_VERSION)
                ? BackgroundJobServerThreadType.VirtualThreads
                : BackgroundJobServerThreadType.PlatformThreads;
    }
}
