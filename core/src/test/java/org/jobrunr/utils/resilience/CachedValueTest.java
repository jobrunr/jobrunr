package org.jobrunr.utils.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CachedValueTest {

    @Test
    void testGetCachesValue() {
        final AtomicInteger counter = new AtomicInteger();
        Supplier<String> supplier = () -> "counter-" + counter.incrementAndGet();

        CachedValue<String> cachedValue = new CachedValue<>(supplier, Duration.ofSeconds(2));
        await().during(1, TimeUnit.SECONDS).untilAsserted(() -> assertThat(cachedValue.get()).isEqualTo("counter-1"));
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(cachedValue.get()).isEqualTo("counter-2"));
    }
}