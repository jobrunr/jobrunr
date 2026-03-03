package org.jobrunr.server.threadpool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomizableThreadFactory implements ThreadFactory {
    private final AtomicInteger threadCount = new AtomicInteger();

    private String threadNamePrefix;
    private boolean daemon;

    public CustomizableThreadFactory(String threadNamePrefix, boolean daemon) {
        this.threadNamePrefix = threadNamePrefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, this.threadNamePrefix + "-" + this.threadCount.getAndIncrement());
        thread.setDaemon(daemon);
        return thread;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }
}
