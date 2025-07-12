package org.jobrunr.scheduling.interval;

import org.jobrunr.scheduling.ScheduleExpressionType;
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
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class IntervalTest {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Instant createdAtNotRelevantInstant = Instant.ofEpochSecond(0);

    private static final String FIVE_SECONDS = "PT5S";
    private static final String TEN_SECONDS = "PT10S";
    private static final String TEN_HOURS = "PT10H";
    private static final String FORTY_EIGHT_HOURS = "PT48H";
    private static final String EIGHT_DAYS = "P8D";

    @ParameterizedTest
    @MethodSource("startInstantDurationAndResultInstant")
    void testInterval(String durationExpression, String baseDateTime, String currentDateTime, String expectedDateTime) {
        try {
            Instant baseInstant = baseDateTime.contains("T") ? Instant.parse(baseDateTime) : LocalDateTime.parse(baseDateTime, dateTimeFormatter).toInstant(UTC);
            Instant currentInstant = currentDateTime.contains("T") ? Instant.parse(currentDateTime) : LocalDateTime.parse(currentDateTime, dateTimeFormatter).toInstant(UTC);
            Instant expectedInstant = expectedDateTime.contains("T") ? Instant.parse(expectedDateTime) : LocalDateTime.parse(expectedDateTime, dateTimeFormatter).toInstant(UTC);

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
    void toStringForDuration() {
        assertThat(new Interval(Duration.ofHours(1)).toString()).isEqualTo("PT1H");
        assertThat(new Interval("PT1H [PT1H/PT1H]").toString()).isEqualTo("PT1H [PT1H/PT1H]");
    }

    @Test
    void toStringCanBeParsedBackIntoInterval() {
        var pt1h = new Interval(Duration.ofHours(1)).toString();
        assertThat(ScheduleExpressionType.createScheduleFromString(pt1h).toString()).isEqualTo(pt1h);

        var pt1hWithMargin = new Interval("PT1H [PT1H/PT1H]").toString();
        assertThat(ScheduleExpressionType.createScheduleFromString(pt1hWithMargin).toString()).isEqualTo(pt1hWithMargin);
    }

    @Test
    void intervalsAreScheduledIndependentlyOfZoneId() {
        int hour = 8;
        Instant now = Instant.now();

        Instant actualNextInstant1 = new Interval(Duration.ofHours(hour)).next(createdAtNotRelevantInstant, now, ZoneId.of("+02:00"));
        Instant actualNextInstant2 = new Interval(Duration.ofHours(hour)).next(createdAtNotRelevantInstant, now, UTC);

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
                .describedAs("Expecting %s to be less than %s. Current LocalDateTime", interval1.next(createdAtNotRelevantInstant, now, UTC).toString(), interval2.next(createdAtNotRelevantInstant, now, UTC).toString(), now.toString())
                .isLessThan(interval2);
    }

    static Stream<Arguments> startInstantDurationAndResultInstant() {
        Instant createdAt1 = Instant.parse("2024-11-19T04:01:57Z");
        Instant createdAt2 = Instant.parse("2024-11-20T04:01:57Z");
        Instant createdAt3 = createdAt2.plus(Duration.ofHours(10));

        return Stream.of(
                // TEST IDEMPOTENCY 1
                arguments(TEN_HOURS, createdAt1.toString(), createdAt1.toString(), createdAt1.plus(10, HOURS).toString()),
                arguments(TEN_HOURS, createdAt1.toString(), createdAt1.minusSeconds(5).toString(), createdAt1.plus(10, HOURS).toString()),
                arguments(TEN_HOURS, createdAt1.toString(), "2024-11-20T14:34:07Z", "2024-11-20T20:01:57Z"),
                arguments(TEN_HOURS, createdAt1.toString(), "2024-11-20T20:01:57Z", "2024-11-21T06:01:57Z"),
                arguments(TEN_HOURS, createdAt1.toString(), "2024-11-20T10:01:52Z", "2024-11-20T10:01:57Z"),
                arguments(TEN_HOURS, createdAt1.toString(), "2024-11-20T10:01:57Z", "2024-11-20T20:01:57Z"),

                // TEST IDEMPOTENCY 2
                arguments(TEN_HOURS, createdAt2.toString(), createdAt2.toString(), createdAt2.plus(10, HOURS).toString()),
                arguments(TEN_HOURS, createdAt2.toString(), createdAt2.minus(5, HOURS).toString(), createdAt2.plus(10, HOURS).toString()),
                arguments(TEN_HOURS, createdAt2.toString(), "2024-11-20T14:34:07Z", "2024-11-21T00:01:57Z"),
                arguments(TEN_HOURS, createdAt2.toString(), "2024-11-21T00:01:57Z", "2024-11-21T10:01:57Z"),
                arguments(TEN_HOURS, createdAt2.toString(), "2024-11-20T14:01:52Z", "2024-11-20T14:01:57Z"),
                arguments(TEN_HOURS, createdAt2.toString(), "2024-11-20T14:01:57Z", "2024-11-21T00:01:57Z"),

                // TEST IDEMPOTENCY 3
                arguments(TEN_HOURS, createdAt3.toString(), createdAt3.toString(), createdAt3.plus(10, HOURS).toString()),
                arguments(TEN_HOURS, createdAt3.toString(), createdAt3.minus(5, HOURS).toString(), createdAt3.plus(10, HOURS).toString()),
                arguments(TEN_HOURS, createdAt3.toString(), "2024-11-20T14:04:07Z", "2024-11-21T00:01:57Z"),
                arguments(TEN_HOURS, createdAt3.toString(), "2024-11-21T00:01:57Z", "2024-11-21T10:01:57Z"),
                arguments(TEN_HOURS, createdAt3.toString(), "2024-11-20T14:01:52Z", "2024-11-21T00:01:57Z"),
                arguments(TEN_HOURS, createdAt3.toString(), "2024-11-21T00:01:57Z", "2024-11-21T10:01:57Z"),

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
