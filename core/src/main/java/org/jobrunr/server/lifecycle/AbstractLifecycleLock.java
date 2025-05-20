package org.jobrunr.server.lifecycle;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractLifecycleLock implements AutoCloseable {

    protected final BackgroundJobServerLifecycle backgroundJobServerLifecycle;

    public AbstractLifecycleLock(BackgroundJobServerLifecycle backgroundJobServerLifecycle) {
        lock(readWriteLock(backgroundJobServerLifecycle));
        this.backgroundJobServerLifecycle = backgroundJobServerLifecycle;
    }

    protected abstract void lock(ReentrantReadWriteLock readWriteLock);

    protected abstract void unlock(ReentrantReadWriteLock readWriteLock);

    @Override
    public void close() {
        unlock(readWriteLock());
    }

    private ReentrantReadWriteLock readWriteLock() {
        return readWriteLock(backgroundJobServerLifecycle);
    }

    private ReentrantReadWriteLock readWriteLock(BackgroundJobServerLifecycle backgroundJobServerLifecycle) {
        return backgroundJobServerLifecycle.readWriteLock;
    }
}
