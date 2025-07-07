package org.jobrunr.utils.uuid;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDv7FactoryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UUIDv7FactoryTest.class);

    @Test
    void testNoCollisions() throws InterruptedException {
        int threadCount = 64;
        int iterationCount = 20_000;
        ConcurrentSkipListMap<UUID, Long> uuidMap = new ConcurrentSkipListMap<>();
        AtomicLong collisionCount = new AtomicLong();
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        UUIDv7Factory uuiDv7Factory = UUIDv7Factory.builder().withIncrementPlus1().build();


        Thread thread = new Thread(() -> evictOldestEntries(uuidMap, 1_000_000, endLatch));
        thread.start();

        long startNanos = System.nanoTime();
        for (long i = 0; i < threadCount; i++) {
            long threadId = i;
            new Thread(() -> {
                for (long j = 0; j < iterationCount; j++) {
                    UUID uuid = uuiDv7Factory.create();
                    Long existingUUID = uuidMap.put(uuid, j);
                    if (j % 10_000 == 0) {
                        LOGGER.trace("Thread {} has {} iterations (collision count {}) / UUID Map Size: {}", threadId, j, collisionCount.get(), uuidMap.size());
                    }
                    if (existingUUID != null) {
                        collisionCount.incrementAndGet();
                        while (endLatch.getCount() > 0) {
                            endLatch.countDown();
                        }
                        throw new IllegalStateException("Collision for uuid " + uuid);
                    }
                }
                endLatch.countDown();
            }).start();
        }
        endLatch.await();
        LOGGER.info("{} UUIDs generated / {} in {}", uuidMap.size(), collisionCount.get(), Duration.ofNanos(System.nanoTime() - startNanos));
        assertThat(collisionCount.get()).isZero();
    }

    private void evictOldestEntries(NavigableMap<UUID, Long> uuidMap, int amountToKeep, CountDownLatch endLatch) {
        try {
            while (endLatch.getCount() > 0) {
                // keep removing the lowest (oldest) entry until size ≤ MAX_ENTRIES
                while (uuidMap.size() > amountToKeep) {
                    Map.Entry<?, ?> removed = uuidMap.pollFirstEntry();
                    if (removed == null) break;
                    // optionally: do something with removed.getValue()
                }
                Thread.sleep(25);
            }
        } catch (Exception e) {
            // log and swallow to keep scheduler alive
            LOGGER.error("Error during eviction", e);
        }
    }
}