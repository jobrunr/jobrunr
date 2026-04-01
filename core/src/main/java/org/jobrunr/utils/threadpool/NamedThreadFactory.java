package org.jobrunr.utils.threadpool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private final AtomicInteger threadCount = new AtomicInteger();
    private final String threadNamePrefix;
    private final boolean daemon;

    public NamedThreadFactory(String threadNamePrefix, boolean daemon) {
        this.threadNamePrefix = threadNamePrefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, this.threadNamePrefix + "-" + this.threadCount.getAndIncrement());
        thread.setDaemon(daemon);
        return thread;
    }
}
