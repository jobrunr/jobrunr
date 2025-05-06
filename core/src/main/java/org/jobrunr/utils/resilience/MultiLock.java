package org.jobrunr.utils.resilience;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class MultiLock implements Closeable {

    private final Collection<Lock> locks;

    public MultiLock(Lockable... lockables) {
        this(Arrays.asList(lockables));
    }

    public MultiLock(Collection<? extends Lockable> lockables) {
        this.locks = lockables.stream().map(Lockable::lock).collect(toList());
    }

    public void unlock() {
        locks.forEach(Lock::unlock);
    }

    @Override
    public void close() {
        unlock();
    }
}
