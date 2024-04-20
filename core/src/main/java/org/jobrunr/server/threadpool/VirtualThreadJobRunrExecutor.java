package org.jobrunr.server.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.jobrunr.utils.reflection.ReflectionUtils.findMethod;

public class VirtualThreadJobRunrExecutor implements JobRunrExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualThreadJobRunrExecutor.class);

    private final ExecutorService executorService;
    private final int workerCount;
    private boolean started;
    private boolean isStopping;

    public VirtualThreadJobRunrExecutor(int workerCount) {
        this(workerCount, "backgroundjob-worker");
    }

    public VirtualThreadJobRunrExecutor(int workerCount, String name) {
        this(workerCount, createVirtualThreadExecutorService(name));
    }

    public VirtualThreadJobRunrExecutor(int workerCount, ExecutorService executorService) {
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
    public void stop(Duration awaitTimeout) {
        this.isStopping = true;
        this.started = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(awaitTimeout.getSeconds(), TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
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

    static ExecutorService createVirtualThreadExecutorService(String name) {
        try {
            Method virtualThreadBuilderMethod = findMethod(Thread.class, "ofVirtual").orElseThrow(() -> new NoSuchMethodException("java.lang.Thread.ofVirtual()"));
            Object virtualThreadBuilder = virtualThreadBuilderMethod.invoke(null);

            Method nameVirtualThreadBuilderMethod = findMethod(virtualThreadBuilderMethod.getReturnType(), "name", String.class).orElseThrow(() -> new NoSuchMethodException("java.lang.Thread.Builder.OfVirtual.name(java.lang.String)"));
            virtualThreadBuilder = nameVirtualThreadBuilderMethod.invoke(virtualThreadBuilder, name);

            Method factoryVirtualThreadBuilderMethod = findMethod(Class.forName("java.lang.Thread$Builder"), "factory").orElseThrow(() -> new NoSuchMethodException("java.lang.Thread.Builder.OfVirtual.factory()"));
            ThreadFactory factory = (ThreadFactory) factoryVirtualThreadBuilderMethod.invoke(virtualThreadBuilder);
            Method newThreadPerTaskExecutorMethod = findMethod(Executors.class, "newThreadPerTaskExecutor", ThreadFactory.class).orElseThrow(() -> new NoSuchMethodException("java.util.concurrent.Executors.newThreadPerTaskExecutor(java.util.concurrent.ThreadFactory)"));
            return (ExecutorService) newThreadPerTaskExecutorMethod.invoke(null, factory);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create VirtualThreadJobRunrExecutor on Java " + System.getProperty("java.version"), e);
        }
    }
}
