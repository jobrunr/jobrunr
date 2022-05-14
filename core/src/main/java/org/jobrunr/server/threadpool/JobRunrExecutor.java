package org.jobrunr.server.threadpool;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

public interface JobRunrExecutor extends Executor {

    int getPriority();

    void start();

    void stop();

    void scheduleAtFixedRate(Runnable command, Duration initialDelay, Duration period);

    ScheduledFuture<?> schedule(Runnable command, Duration delay);

}
