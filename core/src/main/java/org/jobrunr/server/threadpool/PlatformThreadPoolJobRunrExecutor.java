package org.jobrunr.server.threadpool;

import org.jobrunr.utils.threadpool.NamedThreadFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

public class PlatformThreadPoolJobRunrExecutor extends AbstractJobRunrExecutor<ScheduledThreadPoolExecutor> {

    private final int corePoolSize;
    private final Map<Runnable, ScheduledFuture<?>> scheduledFutures;

    public PlatformThreadPoolJobRunrExecutor(int corePoolSize) {
        this(corePoolSize, "backgroundjob-worker-pool");
    }

    public PlatformThreadPoolJobRunrExecutor(int corePoolSize, String threadNamePrefix) {
        this(corePoolSize, corePoolSize, threadNamePrefix);
    }

    public PlatformThreadPoolJobRunrExecutor(int corePoolSize, int maxPoolSize, String threadNamePrefix) {
        super(corePoolSize, createPlatformThreadExecutorService(corePoolSize, maxPoolSize, threadNamePrefix));
        this.corePoolSize = corePoolSize;
        this.scheduledFutures = new HashMap<>();
    }

    public void increasePoolSize(int increment) {
        if (increment <= 0) throw new IllegalArgumentException("increment must be greater than zero");
        executorService.setMaximumPoolSize(executorService.getMaximumPoolSize() + increment);
        executorService.setCorePoolSize(executorService.getCorePoolSize() + increment);
    }

    public void scheduleWithFixedDelay(Runnable command, Duration initialDelay, Duration delayBetweenRuns) {
        ScheduledFuture<?> scheduledFuture = executorService.scheduleWithFixedDelay(command, initialDelay.toMillis(), delayBetweenRuns.toMillis(), TimeUnit.MILLISECONDS);
        scheduledFutures.put(command, scheduledFuture);
    }

    public <T extends Runnable> void cancelScheduledFuturesOfType(Class<T> type) {
        List<Runnable> toCancel = scheduledFutures.keySet().stream()
                .filter(x -> type.isAssignableFrom(x.getClass()))
                .collect(toList());

        for (Runnable runnable : toCancel) {
            ScheduledFuture<?> future = scheduledFutures.remove(runnable);
            future.cancel(true);
            executorService.remove(runnable);
        }

        executorService.purge();
        int newPoolSize = getNewPoolSize(toCancel);
        executorService.setCorePoolSize(newPoolSize);
        executorService.setMaximumPoolSize(newPoolSize);
    }

    static ScheduledThreadPoolExecutor createPlatformThreadExecutorService(int corePoolSize, int maxPoolSize, String threadNamePrefix) {
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory(threadNamePrefix, false);
        ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(corePoolSize, namedThreadFactory);
        executorService.setMaximumPoolSize(maxPoolSize);
        executorService.setRemoveOnCancelPolicy(true);
        return executorService;
    }

    private int getNewPoolSize(List<Runnable> toCancel) {
        int currentCorePoolSize = executorService.getCorePoolSize();
        int reduction = toCancel.size();
        return Math.max(this.corePoolSize, currentCorePoolSize - reduction);
    }
}
