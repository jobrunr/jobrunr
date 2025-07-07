package org.jobrunr.utils.resilience;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CachedValue<T> {
    private static class Entry<U> {
        final U value;
        final long timestamp;

        Entry(U value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    private final Supplier<T> supplier;
    private final long ttlMillis;
    private final AtomicReference<Entry<T>> cache = new AtomicReference<>();

    /**
     * @param supplier how to fetch the fresh value
     * @param ttl      how long (maximum) to keep the cached value before refreshing
     */
    public CachedValue(Supplier<T> supplier, Duration ttl) {
        this.supplier = supplier;
        this.ttlMillis = ttl.toMillis();
    }

    /**
     * Returns the cached value if it’s still “fresh”; otherwise recomputes, caches, and returns it.
     */
    public T get() {
        long now = System.currentTimeMillis();
        Entry<T> current = cache.get();

        if (current == null || now - current.timestamp > ttlMillis) {
            T fresh = supplier.get();
            Entry<T> next = new Entry<>(fresh, now);
            if (cache.compareAndSet(current, next)) {
                return fresh;
            } else {
                return cache.get().value;
            }
        }

        return current.value;
    }
}
