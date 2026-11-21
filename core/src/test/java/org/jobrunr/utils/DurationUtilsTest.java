package org.jobrunr.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DurationUtilsTest {

    @Test
    void fromBigDecimalConvertsDecimalSecondsWithScaleSmallerThanNine() {
        assertThat(DurationUtils.fromBigDecimal(new BigDecimal("1.5"))).isEqualTo(Duration.ofMillis(1500));
    }

    @Test
    void fromBigDecimalConvertsNegativeDecimalSeconds() {
        assertThat(DurationUtils.fromBigDecimal(new BigDecimal("-1.500000000"))).isEqualTo(Duration.ofMillis(-1500));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1500, -1000, -1, 0, 1, 1000, 1500, 90_000_000L})
    void fromBigDecimalIsTheInverseOfToBigDecimal(long millis) {
        Duration duration = Duration.ofMillis(millis);
        assertThat(DurationUtils.fromBigDecimal(DurationUtils.toBigDecimal(duration))).isEqualTo(duration);
    }
}
