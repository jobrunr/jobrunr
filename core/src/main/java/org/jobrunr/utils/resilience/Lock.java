package org.jobrunr.utils.resilience;

import java.util.concurrent.locks.ReentrantLock;

public class Lock implements AutoCloseable {

    private final ReentrantLock reentrantLock;

    public Lock() {
        this.reentrantLock = new ReentrantLock();
    }

    public Lock lock() {
        reentrantLock.lock();
        return this;
    }

    public boolean isLocked() {
        return this.reentrantLock.isLocked();
    }

    @Override
    public void close() {
        unlock();
    }

    public void unlock() {
        reentrantLock.unlock();
    }
}
