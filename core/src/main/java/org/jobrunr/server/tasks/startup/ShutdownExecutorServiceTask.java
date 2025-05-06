package org.jobrunr.server.tasks.startup;

import java.util.concurrent.ExecutorService;

public class ShutdownExecutorServiceTask implements Runnable {

    private final ExecutorService executorService;

    public ShutdownExecutorServiceTask(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void run() {
        this.executorService.shutdown();
    }
}
