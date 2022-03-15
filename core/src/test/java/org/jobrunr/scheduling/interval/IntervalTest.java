package org.jobrunr.scheduling.interval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class IntervalTest {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String FIVE_SECONDS = "PT5S";
    private static final String TEN_SECONDS = "PT10S";
    private static final String FORTY_EIGHT_HOURS = "PT48H";
    private static final String EIGHT_DAYS = "P8D";

    @ParameterizedTest
    @MethodSource("startInstantDurationAndResultInstant")
    void testInterval(String durationExpression, String baseDateTime, String currentDateTime, String expectedDateTime) {
        try {
            Instant baseInstant = LocalDateTime.parse(baseDateTime, dateTimeFormatter).toInstant(UTC);
            Instant currentInstant = LocalDateTime.parse(currentDateTime, dateTimeFormatter).toInstant(UTC);
            Instant expectedInstant = LocalDateTime.parse(expectedDateTime, dateTimeFormatter).toInstant(UTC);

            Interval interval = new Interval(durationExpression);
            Duration duration = Duration.parse(durationExpression);
            Instant nextInstant = interval.next(baseInstant, currentInstant, UTC);

            assertThat(nextInstant)
                    .describedAs("Expecting %s to be after or equal to %s for duration %s and start date %s", nextInstant, currentInstant, duration, baseInstant)
                    .isAfterOrEqualTo(currentInstant);
            assertThat(nextInstant)
                    .describedAs("Expecting %s to be equal to %s for duration %s and start date %s", nextInstant, expectedInstant, duration, baseInstant)
                    .isEqualTo(expectedInstant);

        } catch (Exception e) {
            System.out.printf("Error for %s and %s%n", baseDateTime, durationExpression);
            throw e;
        }
    }

    @Test
    void intervalsAreScheduledIndependentlyOfZoneId() {
        int hour = 8;
        Instant now = Instant.now();

        Instant actualNextInstant1 = new Interval(Duration.ofHours(hour)).next(now, ZoneId.of("+02:00"));
        Instant actualNextInstant2 = new Interval(Duration.ofHours(hour)).next(now, UTC);

        assertThat(actualNextInstant1).isEqualTo(actualNextInstant2);
    }

    @Test
    void intervalsAreEqual() {
        Interval interval1 = new Interval(Duration.ofDays(1));
        Interval interval2 = new Interval(Duration.ofHours(24));

        assertThat(interval1)
                .isEqualTo(interval2)
                .hasSameHashCodeAs(interval2);
    }

    @Test
    void intervalsCanBeCompared() {
        Instant now = Instant.now();

        Interval interval1 = new Interval(Duration.ofHours(23));
        Interval interval2 = new Interval(Duration.ofDays(1));

        assertThat(interval1)
                .describedAs("Expecting %s to be less than %s. Current LocalDateTime", interval1.next(now, UTC).toString(), interval2.next(now, UTC).toString(), now.toString())
                .isLessThan(interval2);
    }

    static Stream<Arguments> startInstantDurationAndResultInstant() {
        return Stream.of(
                arguments(FIVE_SECONDS, "2019-01-01 00:00:00", "2019-01-01 00:00:01", "2019-01-01 00:00:05"),
                arguments(TEN_SECONDS, "2019-01-01 00:00:00", "2019-01-01 00:00:00", "2019-01-01 00:00:10"),
                arguments(TEN_SECONDS, "2019-01-01 00:00:00", "2019-01-01 00:20:05", "2019-01-01 00:20:10"),
                arguments(TEN_SECONDS, "2019-01-01 00:00:09", "2019-01-01 00:00:09", "2019-01-01 00:00:19"),
                arguments(TEN_SECONDS, "2019-01-01 00:58:59", "2019-01-01 00:58:59", "2019-01-01 00:59:09"),
                arguments(TEN_SECONDS, "2019-01-01 00:59:59", "2019-01-01 00:59:59", "2019-01-01 01:00:09"),
                arguments(TEN_SECONDS, "2019-01-01 11:59:59", "2019-01-01 11:59:59", "2019-01-01 12:00:09"),
                arguments(TEN_SECONDS, "2019-01-01 23:59:59", "2019-01-01 23:59:59", "2019-01-02 00:00:09"),
                arguments(TEN_SECONDS, "2021-11-29 23:59:59", "2021-11-29 23:59:59", "2021-11-30 00:00:09"),
                arguments(TEN_SECONDS, "2019-02-28 23:59:59", "2019-02-28 23:59:59", "2019-03-01 00:00:09"),
                arguments(TEN_SECONDS, "2019-12-31 23:59:59", "2019-12-31 23:59:59", "2020-01-01 00:00:09"),
                arguments(TEN_SECONDS, "2020-02-28 23:59:59", "2020-02-28 23:59:59", "2020-02-29 00:00:09"),

                arguments(FORTY_EIGHT_HOURS, "2021-01-01 11:59:59", "2021-01-01 11:59:59", "2021-01-03 11:59:59"),
                arguments(FORTY_EIGHT_HOURS, "2021-11-29 11:59:59", "2021-11-29 11:59:59", "2021-12-01 11:59:59"),
                arguments(FORTY_EIGHT_HOURS, "2021-11-28 11:59:59", "2021-11-28 11:59:59", "2021-11-30 11:59:59"),
                arguments(FORTY_EIGHT_HOURS, "2021-12-28 11:59:59", "2021-12-31 11:59:59", "2022-01-01 11:59:59"),
                arguments(FORTY_EIGHT_HOURS, "2021-12-29 11:59:59", "2021-12-31 11:59:59", "2022-01-02 11:59:59"),

                arguments(EIGHT_DAYS, "2021-01-01 11:59:59", "2021-01-01 11:59:59", "2021-01-09 11:59:59"),
                arguments(EIGHT_DAYS, "2021-11-29 11:59:59", "2021-11-29 11:59:59", "2021-12-07 11:59:59"),
                arguments(EIGHT_DAYS, "2021-11-28 11:59:59", "2021-11-28 11:59:59", "2021-12-06 11:59:59"),
                arguments(EIGHT_DAYS, "2020-02-28 11:59:59", "2020-02-28 23:59:59", "2020-03-07 11:59:59"),
                arguments(EIGHT_DAYS, "2021-02-28 11:59:59", "2021-02-28 11:59:59", "2021-03-08 11:59:59")
        );
    }
}
