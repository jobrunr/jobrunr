package org.jobrunr.server.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ScheduledThreadPoolJobRunrExecutor extends ScheduledThreadPoolExecutor implements JobRunrInternalExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledThreadPoolJobRunrExecutor.class);

    private final AntiDriftThread antiDriftThread;

    public ScheduledThreadPoolJobRunrExecutor(
      final int corePoolSize,
      final String threadNamePrefix) {
        super(corePoolSize + 1, new NamedThreadFactory(threadNamePrefix));

        setMaximumPoolSize((corePoolSize + 1) * 2);
        setKeepAliveTime(1, TimeUnit.MINUTES);

        antiDriftThread = new AntiDriftThread(this);
        antiDriftThread.setDaemon(true);
        antiDriftThread.start();
    }

    @Override
    public void scheduleAtFixedRate(final Runnable command,
                                    final Duration initialDelay,
                                    final Duration period) {
        antiDriftThread.queue(new AntiDriftSchedule(command, initialDelay, period));
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final Duration delay) {
        return schedule(command, delay.toNanos(), NANOSECONDS);
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
    public synchronized void stop() {
        LOGGER.info("Shutting down ScheduledThreadPoolJobRunrExecutor");
        getQueue().clear();

        shutdown();
        try {
            antiDriftThread.interrupt();
            antiDriftThread.join();

            if (!awaitTermination(10, SECONDS)) {
                shutdownNow();
            }
        } catch (final InterruptedException e) {
            shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
