package org.jobrunr.server.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class PlatformThreadPoolJobRunrExecutor extends java.util.concurrent.ScheduledThreadPoolExecutor implements JobRunrExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformThreadPoolJobRunrExecutor.class);
    private final int workerCount;

    public PlatformThreadPoolJobRunrExecutor(int corePoolSize) {
        this(corePoolSize, "backgroundjob-worker-pool");
    }

    public PlatformThreadPoolJobRunrExecutor(int corePoolSize, String threadNamePrefix) {
        super(corePoolSize, new NamedThreadFactory(threadNamePrefix));
        this.workerCount = corePoolSize;
        setMaximumPoolSize(corePoolSize * 2);
        setKeepAliveTime(1, TimeUnit.MINUTES);
    }

    @Override
    public int getWorkerCount() {
        return workerCount;
    }

    @Override
    public void start() {
        this.prestartAllCoreThreads();
        LOGGER.info("ThreadManager of type 'ScheduledThreadPool' started");
    }

    @Override
    public void stop() {
        shutdown();
        try {
            if (!awaitTermination(10, TimeUnit.SECONDS)) {
                shutdownNow();
            }
        } catch (InterruptedException e) {
            shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isStopping() {
        return isTerminating() || isTerminated();
    }

    private static class NamedThreadFactory implements ThreadFactory {

        private final String poolName;
        private final ThreadFactory threadFactory;

        public NamedThreadFactory(String poolName) {
            this.poolName = poolName;
            threadFactory = Executors.defaultThreadFactory();
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = threadFactory.newThread(runnable);
            thread.setName(thread.getName().replace("pool", poolName));
            return thread;
        }
    }
}
