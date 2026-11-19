package org.jobrunr.utils;

import org.jobrunr.utils.annotations.Because;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.Stream;

import static java.time.Duration.ofNanos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DurationUtilsTest {

    @ParameterizedTest
    @MethodSource("durationsToBigDecimal")
    void toBigDecimal(Duration duration, String expected) {
        assertThat(DurationUtils.toBigDecimal(duration)).isEqualByComparingTo(expected);
    }

    static Stream<Arguments> durationsToBigDecimal() {
        return Stream.of(
                // whole seconds
                arguments(Duration.ZERO, "0"),
                arguments(Duration.ofSeconds(1), "1"),
                arguments(Duration.ofSeconds(2), "2"),
                arguments(Duration.ofSeconds(-1), "-1"),
                arguments(Duration.ofDays(365_000), "31536000000"),
                arguments(Duration.ofSeconds(1_000_000_000_000L), "1000000000000"),
                arguments(Duration.ofSeconds(-1_000_000_000_000L), "-1000000000000"),

                // fractions of a second
                arguments(ofNanos(1), "0.000000001"),
                arguments(ofNanos(-1), "-0.000000001"),
                arguments(ofNanos(999_999_999), "0.999999999"),
                arguments(Duration.ofMillis(50), "0.050000000"),
                arguments(Duration.ofMillis(-50), "-0.050000000"),
                arguments(Duration.ofMillis(1500), "1.500000000"),
                arguments(Duration.ofMillis(-1500), "-1.500000000"),
                arguments(Duration.ofSeconds(123_456_789, 123_456_789), "123456789.123456789"),

                // the extremes of the Duration range
                arguments(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), "9223372036854775807.999999999"),
                arguments(Duration.ofSeconds(Long.MIN_VALUE), "-9223372036854775808")
        );
    }

    @ParameterizedTest
    @MethodSource("bigDecimalsToDurations")
    @Because("https://github.com/jobrunr/jobrunr/issues/1642")
    void fromBigDecimal(String durationAsBigDecimal, Duration expected) {
        assertThat(DurationUtils.fromBigDecimal(new BigDecimal(durationAsBigDecimal))).isEqualTo(expected);
    }

    static Stream<Arguments> bigDecimalsToDurations() {
        return Stream.of(
                // zero, in different notations
                arguments("0", Duration.ZERO),
                arguments("0.0", Duration.ZERO),
                arguments("0E-9", Duration.ZERO),

                // whole seconds
                arguments("1", Duration.ofSeconds(1)),
                arguments("1.0", Duration.ofSeconds(1)),
                arguments("2", Duration.ofSeconds(2)),
                arguments("-1", Duration.ofSeconds(-1)),
                arguments("31536000000", Duration.ofDays(365_000)),
                arguments("1000000000000", Duration.ofSeconds(1_000_000_000_000L)),

                // the same value with an increasing number of decimals (scale)
                arguments("1.5", Duration.ofMillis(1500)),
                arguments("1.50", Duration.ofMillis(1500)),
                arguments("1.500", Duration.ofMillis(1500)),
                arguments("1.5000", Duration.ofMillis(1500)),
                arguments("1.50000", Duration.ofMillis(1500)),
                arguments("1.500000", Duration.ofMillis(1500)),
                arguments("1.5000000", Duration.ofMillis(1500)),
                arguments("1.50000000", Duration.ofMillis(1500)),
                arguments("1.500000000", Duration.ofMillis(1500)),
                arguments("1.5000000000", Duration.ofMillis(1500)),

                // negative durations should keep their sign
                arguments("-1.5", Duration.ofMillis(-1500)),
                arguments("-1.500000000", Duration.ofMillis(-1500)),
                arguments("-0.050000000", Duration.ofMillis(-50)),
                arguments("-1E-9", ofNanos(-1)),

                // fractions of a second
                arguments("0.05", Duration.ofMillis(50)),
                arguments("0.050000000", Duration.ofMillis(50)),
                arguments("123456789.123456789", Duration.ofSeconds(123_456_789, 123_456_789)),

                // the extremes of the Duration range
                arguments("9223372036854775807.999999999", Duration.ofSeconds(Long.MAX_VALUE, 999_999_999)),
                arguments("-9223372036854775808", Duration.ofSeconds(Long.MIN_VALUE))
        );
    }
}
