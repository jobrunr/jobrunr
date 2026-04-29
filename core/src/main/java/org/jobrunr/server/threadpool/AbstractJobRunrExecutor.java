package org.jobrunr.server.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractJobRunrExecutor<T extends ExecutorService> implements JobRunrExecutor {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final int workerCount;
    protected final T executorService;
    private boolean started;

    public AbstractJobRunrExecutor(int workerCount, T executorService) {
        this.workerCount = workerCount;
        this.executorService = executorService;
    }

    @Override
    public void start() {
        this.started = true;
        LOGGER.info("ThreadManager of type '{}' started", this.getClass().getSimpleName());
    }

    @Override
    public int getWorkerCount() {
        return workerCount;
    }

    @Override
    public void execute(Runnable command) {
        if (started) {
            executorService.execute(command);
        }
    }

    @Override
    public void stop(Duration awaitTimeout) {
        this.started = false;
        executorService.shutdown();
        Duration backgroundJobPerformerSaveDuration = Duration.ofSeconds(1);
        Duration executorStopDuration = awaitTimeout.minus(backgroundJobPerformerSaveDuration);
        executorStopDuration = executorStopDuration.isNegative() ? Duration.ZERO : executorStopDuration;
        try {
            if (!executorService.awaitTermination(executorStopDuration.toMillis(), TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
            if (!executorService.awaitTermination(backgroundJobPerformerSaveDuration.toMillis(), TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Could not save jobs in StorageProvider as they did not interrupt after shutdown");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
    }
}
