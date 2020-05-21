package org.jobrunr.utils.resilience;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

class RateLimiterTest {

    @Test
    void testRateLimit() {
        final RateLimiter rateLimit = rateLimit().at1Request().per(SECOND);
        await()
                .pollInterval(ofMillis(20))
                .atMost(ofMillis(150))
                .untilAsserted(() -> {
                    assertThat(rateLimit.isAllowed()).isTrue();
                    assertThat(rateLimit.isAllowed()).isFalse();
                });
        await()
                .pollInterval(ofMillis(20))
                .atMost(ofMillis(1050))
                .untilAsserted(() -> assertThat(rateLimit.isAllowed()).isTrue());
    }

    @Test
    void testRateLimit8PerSecond() {
        final RateLimiter rateLimit = rateLimit().atRequests(8).per(SECOND);

        List<Boolean> allowed = new ArrayList<>();

        await()
                .pollInterval(ofMillis(20))
                .atMost(ofMillis(1050))
                .untilAsserted(() -> {
                    allowed.add(rateLimit.isAllowed());
                    assertThat(allowed.stream().filter(x -> x)).hasSize(8);
                });
    }

}