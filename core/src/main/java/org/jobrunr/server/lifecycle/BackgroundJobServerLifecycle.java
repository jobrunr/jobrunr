package org.jobrunr.server.lifecycle;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BackgroundJobServerLifecycle {

    final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    final Condition readWriteFinished = readWriteLock.writeLock().newCondition();
    volatile BackgroundJobServerLifecycleEvent lifecycleEvent;
    volatile boolean isRunning = false;

    public LifecycleChangeLock goTo(BackgroundJobServerLifecycleEvent event) {
        return new LifecycleChangeLock(this, event);
    }

    public LifecycleReadLock readLock() {
        return new LifecycleReadLock(this);
    }

    public boolean isTransitioning() {
        return readWriteLock.isWriteLocked();
    }

    public boolean isTransitioningTo(@Nullable BackgroundJobServerLifecycleEvent event) {
        if (!isTransitioning()) return false;

        try {
            if (event == null) {
                readWriteFinished.await(10, TimeUnit.MILLISECONDS);
            }
            return Objects.equals(event, lifecycleEvent);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
