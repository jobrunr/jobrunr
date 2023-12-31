package org.jobrunr.server.threadpool;

import java.util.concurrent.Executor;

public interface JobRunrExecutor extends Executor {

    int getWorkerCount();

    void start();

    void stop();

    boolean isStopping();
}
