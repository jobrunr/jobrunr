package org.jobrunr.server.lifecycle;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LifecycleReadLock extends AbstractLifecycleLock {

    public LifecycleReadLock(BackgroundJobServerLifecycle backgroundJobServerLifecycle) {
        super(backgroundJobServerLifecycle);
    }

    @Override
    protected void lock(ReentrantReadWriteLock readWriteLock) {
        readWriteLock.readLock().lock();
    }

    @Override
    protected void unlock(ReentrantReadWriteLock readWriteLock) {
        readWriteLock.readLock().unlock();
    }

}
