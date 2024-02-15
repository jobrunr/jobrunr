package org.jobrunr.server.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.jobrunr.utils.reflection.ReflectionUtils.getMethod;

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

    static ExecutorService createVirtualThreadExecutorService(String name) {
        try {
            Method virtualThreadBuilderMethod = getMethod(Thread.class, "ofVirtual");
            Object virtualThreadBuilder = virtualThreadBuilderMethod.invoke(null);

            Method nameVirtualThreadBuilderMethod = getMethod(virtualThreadBuilderMethod.getReturnType(), "name", String.class);
            virtualThreadBuilder = nameVirtualThreadBuilderMethod.invoke(virtualThreadBuilder, name);

            Method factoryVirtualThreadBuilderMethod = getMethod(Class.forName("java.lang.Thread$Builder"), "factory");
            ThreadFactory factory = (ThreadFactory) factoryVirtualThreadBuilderMethod.invoke(virtualThreadBuilder);
            Method newThreadPerTaskExecutorMethod = getMethod(Executors.class, "newThreadPerTaskExecutor", ThreadFactory.class);
            return (ExecutorService) newThreadPerTaskExecutorMethod.invoke(null, factory);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create VirtualThreadJobRunrExecutor on Java " + System.getProperty("java.version"), e);
        }
    }
}
