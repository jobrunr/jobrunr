package org.jobrunr.server.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.jobrunr.utils.reflection.ReflectionUtils.getMethod;

public class VirtualThreadPoolJobRunrExecutor implements JobRunrExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualThreadPoolJobRunrExecutor.class);

    private final ExecutorService executorService;
    private final int workerCount;
    private boolean started;
    private boolean isStopping;

    public VirtualThreadPoolJobRunrExecutor(int workerCount) {
        this(workerCount, createVirtualThreadExecutorService());
    }

    public VirtualThreadPoolJobRunrExecutor(int workerCount, ExecutorService executorService) {
        this.workerCount = workerCount;
        this.executorService = executorService;
    }

    @Override
    public int getWorkerCount() {
        return workerCount;
    }

    @Override
    public void start() {
        this.started = true;
        LOGGER.info("ThreadManager of type 'VirtualThreadPerTask' started");
    }

    @Override
    public void stop() {
        this.isStopping = true;
        this.started = false;
    }

    @Override
    public boolean isStopping() {
        return isStopping;
    }

    @Override
    public void execute(Runnable command) {
        if (started) {
            executorService.submit(command);
        }
    }

    static ExecutorService createVirtualThreadExecutorService() {
        try {
            Method newVirtualThreadPerTaskExecutor = getMethod(Executors.class, "newVirtualThreadPerTaskExecutor");
            return (ExecutorService) newVirtualThreadPerTaskExecutor.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Expected Executors.newVirtualThreadPerTaskExecutor to be present on Java " + System.getProperty("java.version"));
        }
    }
}
