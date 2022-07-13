package org.jobrunr.server.threadpool;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

interface JobRunrInternalExecutor extends JobRunrExecutor {

    ScheduledFuture<?> schedule(Runnable command, Duration delay);

}
