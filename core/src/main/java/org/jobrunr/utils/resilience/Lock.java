package org.jobrunr.utils.resilience;

import java.util.concurrent.Semaphore;

public class Lock implements AutoCloseable {

    private final Semaphore semaphore;

    public Lock() {
        this.semaphore = new Semaphore(1);
    }

    public Lock lock() {
        semaphore.acquireUninterruptibly();
        return this;
    }

    public boolean isLocked() {
        return this.semaphore.availablePermits() < 1;
    }

    public void close() {
        unlock();
    }

    public void unlock() {
        semaphore.release();
    }
}
