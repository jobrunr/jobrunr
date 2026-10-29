package org.jobrunr.server.threadpool;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.jobrunr.utils.reflection.ReflectionUtils.findMethod;

public class VirtualThreadJobRunrExecutor extends AbstractJobRunrExecutor<ExecutorService> {

    public VirtualThreadJobRunrExecutor(int workerCount) {
        this(workerCount, "backgroundjob-worker");
    }

    public VirtualThreadJobRunrExecutor(int workerCount, String name) {
        this(workerCount, createVirtualThreadExecutorService(name));
    }

    VirtualThreadJobRunrExecutor(int workerCount, ExecutorService executorService) {
        super(workerCount, executorService);
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
