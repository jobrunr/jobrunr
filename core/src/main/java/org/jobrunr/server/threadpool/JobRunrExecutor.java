package org.jobrunr.server.threadpool;

import java.util.concurrent.Executor;

public interface JobRunrExecutor extends Executor {

    int getPriority();

    void start();

    void stop();

}
