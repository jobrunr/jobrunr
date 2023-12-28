package org.jobrunr.server.threadpool;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.jobrunr.utils.reflection.ReflectionUtils.getMethod;

public class VirtualThreadPoolJobRunrExecutor implements JobRunrExecutor {

    private final ExecutorService executorService;
    private boolean started;

    public VirtualThreadPoolJobRunrExecutor() {
        executorService = createVirtualThreadExecutorService();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void start() {
        this.started = true;
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public void execute(Runnable command) {
        if (started) {
            executorService.submit(command);
        }
    }

    ExecutorService createVirtualThreadExecutorService() {
        try {
            Method newVirtualThreadPerTaskExecutor = getMethod(Executors.class, "newVirtualThreadPerTaskExecutor");
            return (ExecutorService) newVirtualThreadPerTaskExecutor.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Expected Executors.newVirtualThreadPerTaskExecutor to be present on Java " + System.getProperty("java.version"));
        }
    }
}
