package org.jobrunr.server.threadpool;

import java.util.concurrent.Executor;

public interface JobRunrExecutor extends Executor {

    Integer getPriority();

    void start();

    void stop();

}
