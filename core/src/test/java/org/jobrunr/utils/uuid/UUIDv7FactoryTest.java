package org.jobrunr.utils.uuid;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class UUIDv7FactoryTest {
    
    @Test
    void testNoCollisions() throws InterruptedException {
        int threadCount = 64;
        int iterationCount = 20_000;
        Map<UUID, Long> uuidMap = new ConcurrentHashMap<>();
        AtomicLong collisionCount = new AtomicLong();
        long startNanos = System.nanoTime();
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        UUIDv7Factory uuiDv7Factory = UUIDv7Factory.builder().withIncrementPlus1().build();

        for (long i = 0; i < threadCount; i++) {
            long threadId = i;
            new Thread(() -> {
                for (long j = 0; j < iterationCount; j++) {
                    UUID uuid = uuiDv7Factory.create();
                    Long existingUUID = uuidMap.put(uuid, (threadId * iterationCount) + j);
                    if (existingUUID != null) {
                        throw new IllegalStateException("Collision for uuid " + uuid);
                    }
                }
                endLatch.countDown();
            }).start();
        }
        endLatch.await();
        System.out.println(threadCount * iterationCount + " UUIDs generated, " + collisionCount + " collisions in "
                + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos) + "ms");
    }
}