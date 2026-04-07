package org.jobrunr.server.threadpool;

import org.jobrunr.server.JobSteward;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;

public interface JobRunrExecutor extends Executor {

    int getWorkerCount();

    void start();

    default void stop(JobSteward jobSteward, Duration totalInterruptDuration) {
        Duration backgroundJobPerformerSaveDuration = totalInterruptDuration.dividedBy(10);
        Duration executorStopDuration = totalInterruptDuration.minus(backgroundJobPerformerSaveDuration);
        stop(executorStopDuration);
        Instant deadline = Instant.now().plus(backgroundJobPerformerSaveDuration);
        while (jobSteward.getOccupiedWorkerCount() > 0 && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    void stop(Duration awaitTimeout);

    boolean isStopping();
}
