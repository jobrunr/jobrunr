package org.jobrunr.server.lifecycle;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BackgroundJobServerLifecycle {

    final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    volatile BackgroundJobServerLifecycleEvent lifecycleEvent;
    volatile boolean isRunning = false;

    public LifecycleChangeLock goTo(BackgroundJobServerLifecycleEvent event) {
        return new LifecycleChangeLock(this, event);
    }

    public LifecycleReadLock readLock() {
        return new LifecycleReadLock(this);
    }

    public boolean isTransitioning() {
        return reentrantReadWriteLock.isWriteLocked();
    }

    public boolean isTransitioningTo(BackgroundJobServerLifecycleEvent event) {
        return isTransitioning() && Objects.equals(event, lifecycleEvent);
    }

    public boolean isRunning() {
        return isRunning;
    }
}
