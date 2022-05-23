package org.jobrunr.server.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPoolJobRunrExecutor extends java.util.concurrent.ScheduledThreadPoolExecutor implements JobRunrExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledThreadPoolJobRunrExecutor.class);

    private final AntiDriftScheduler antiDriftScheduler;

    public ScheduledThreadPoolJobRunrExecutor(int corePoolSize, String threadNamePrefix) {
        super(corePoolSize + 1, new NamedThreadFactory(threadNamePrefix));
        setMaximumPoolSize((corePoolSize + 1) * 2);
        setKeepAliveTime(1, TimeUnit.MINUTES);
        antiDriftScheduler = new AntiDriftScheduler(this);
        super.scheduleAtFixedRate(antiDriftScheduler, 0, 250, TimeUnit.MILLISECONDS);
    }

    public void scheduleAtFixedRate(Runnable command, Duration initialDelay, Duration period) {
        this.antiDriftScheduler.addSchedule(new AntiDriftSchedule(command, initialDelay, period));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, Duration delay) {
        return this.schedule(command, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public void start() {
        this.prestartAllCoreThreads();
        LOGGER.info("ThreadManager of type 'ScheduledThreadPoolJobRunrExecutor' started");
    }

    @Override
    public void stop() {
        LOGGER.info("Shutting down ScheduledThreadPoolJobRunrExecutor");
        this.antiDriftScheduler.stop();
        this.getQueue().clear();
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
            thread.setDaemon(true);
            return thread;
        }
    }
}
