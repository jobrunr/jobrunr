package org.jobrunr.server.lifecycle;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LifecycleChangeLock extends AbstractLifecycleLock {

    public LifecycleChangeLock(BackgroundJobServerLifecycle backgroundJobServerLifecycle, BackgroundJobServerLifecycleEvent lifecycleEvent) {
        super(backgroundJobServerLifecycle);
        backgroundJobServerLifecycle.lifecycleEvent = lifecycleEvent;
        backgroundJobServerLifecycle.readWriteFinished.signalAll();
    }

    public void succeeded() {
        if (backgroundJobServerLifecycle.lifecycleEvent != null) {
            this.backgroundJobServerLifecycle.isRunning = backgroundJobServerLifecycle.lifecycleEvent.isRunning;
        }
    }

    @Override
    protected void lock(ReentrantReadWriteLock readWriteLock) {
        if (readWriteLock.getReadHoldCount() > 0) throw new IllegalMonitorStateException("Cannot upgrade read to write lock");
        readWriteLock.writeLock().lock();
    }

    @Override
    protected void unlock(ReentrantReadWriteLock readWriteLock) {
        readWriteLock.writeLock().unlock();
    }
}
