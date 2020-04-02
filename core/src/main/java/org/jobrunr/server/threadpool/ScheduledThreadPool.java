package org.jobrunr.server.threadpool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPool extends ScheduledThreadPoolExecutor {

    public ScheduledThreadPool(int corePoolSize, String threadNamePrefix) {
        super(corePoolSize, new NamedThreadFactory(threadNamePrefix));
        setMaximumPoolSize(corePoolSize * 2);
        setKeepAliveTime(1, TimeUnit.MINUTES);
    }

    private static class NamedThreadFactory implements ThreadFactory {

        private final String poolName;
        private final ThreadFactory threadFactory;

        public NamedThreadFactory(String poolName) {
            this.poolName = poolName;
            threadFactory = Executors.defaultThreadFactory();
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = threadFactory.newThread(runnable);
            thread.setName(thread.getName().replace("pool", poolName));
            return thread;
        }
    }
}
