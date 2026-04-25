package org.jobrunr.server.threadpool;

import org.jobrunr.utils.threadpool.NamedThreadFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PlatformThreadPoolJobRunrExecutor extends AbstractJobRunrExecutor<ScheduledThreadPoolExecutor> {

    public PlatformThreadPoolJobRunrExecutor(int corePoolSize) {
        this(corePoolSize, "backgroundjob-worker-pool");
    }

    public PlatformThreadPoolJobRunrExecutor(int corePoolSize, String threadNamePrefix) {
        this(corePoolSize, corePoolSize * 2, threadNamePrefix);
    }

    public PlatformThreadPoolJobRunrExecutor(int corePoolSize, int maxPoolSize, String threadNamePrefix) {
        super(corePoolSize, createPlatformThreadExecutorService(corePoolSize, maxPoolSize, threadNamePrefix));
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return executorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    static ScheduledThreadPoolExecutor createPlatformThreadExecutorService(int corePoolSize, int maxPoolSize, String threadNamePrefix) {
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory(threadNamePrefix, false);
        ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(corePoolSize, namedThreadFactory);
        executorService.setMaximumPoolSize(maxPoolSize);
        executorService.setKeepAliveTime(1, TimeUnit.MINUTES);
        return executorService;
    }
}
