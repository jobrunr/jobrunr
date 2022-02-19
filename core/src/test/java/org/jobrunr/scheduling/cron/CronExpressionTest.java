package org.jobrunr.scheduling.cron;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static java.time.LocalDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CronExpressionTest {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Instant createdAtNotRelevantInstant = Instant.ofEpochSecond(0);

    @ParameterizedTest
    @MethodSource("startInstantCronExpressionAndResultInstant")
    void testCron(String cronExpression, String baseDate, String expectedResult) {
        try {
            Instant inputInstant = LocalDateTime.parse(baseDate, dateTimeFormatter).toInstant(UTC);
            CronExpression cron = CronExpression.create(cronExpression);
            Instant actualInstant = cron.next(createdAtNotRelevantInstant, inputInstant, UTC);
            Instant expectedInstant = LocalDateTime.parse(expectedResult, dateTimeFormatter).toInstant(UTC);

            assertThat(actualInstant)
                    .describedAs("Expecting %s to be equal to %s for cron expression %s and start date %s", actualInstant, expectedInstant, cronExpression, inputInstant)
                    .isEqualTo(expectedInstant);
        } catch (Exception e) {
            System.out.printf("Error for %s, %s and %s%n", baseDate, cronExpression, expectedResult);
            throw e;
        }
    }

    @Test
    void cronExpressionsAreScheduledInUTC() {
        // always use next hour
        int hour = now().getHour() + 1;
        int daysToAdd = hour >= 24 ? 1 : 0;
        hour = hour >= 24 ? 0 : hour;

        Instant actualNextInstant = CronExpression.create(Cron.daily(hour)).next(Instant.now(), UTC);

        Instant expectedNextInstant = OffsetDateTime.of(LocalDate.now().plusDays(daysToAdd), LocalTime.of(hour, 0), UTC).toInstant();

        assertThat(actualNextInstant).isEqualTo(expectedNextInstant);
    }

    // github issue 31
    @Test
    void dailyRecurringJobsTakeTimeZonesCorrectlyIntoAccount() {
        LocalDateTime localDateTime = LocalDateTime.now();
        int hour = localDateTime.getHour();
        int minute = localDateTime.getMinute();
        if (minute < 1) {
            hour = hour - 1;
        } else {
            minute = minute - 1;
        }

        Instant nextRun = CronExpression.create(Cron.daily(hour, minute)).next(Instant.now(), ZoneOffset.of("+02:00"));
        Instant expectedNextRun = now().plusDays(1).withHour(hour).withMinute(minute).withSecond(0).withNano(0).atZone(ZoneOffset.of("+02:00")).toInstant();
        assertThat(nextRun)
                .isAfter(Instant.now())
                .isEqualTo(expectedNextRun);
    }

    // github issue 31
    @Test
    void minutelyRecurringJobsTakeTimeZonesCorrectlyIntoAccount() {
        LocalDateTime localDateTime = LocalDateTime.now();
        int nextMinute = localDateTime.plusMinutes(1).getMinute();

        Instant nextRun = CronExpression.create(Cron.hourly(nextMinute)).next(Instant.now(), ZoneOffset.of("+02:00"));
        assertThat(nextRun).isAfter(Instant.now());
    }

    // github issue 75
    @Test
    void cronExpressionCanBeUsedWithNegativeOffsetTimeZones() {
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneId.of("America/New_York"));
        int nextMinute = offsetDateTime.plusMinutes(1).getMinute();

        Instant nextRun = CronExpression.create(Cron.hourly(nextMinute)).next(Instant.now(), ZoneId.of("America/New_York"));
        assertThat(nextRun)
                .isAfter(Instant.now())
                .isBefore(now().toLocalDate().plusDays(1).atStartOfDay().toInstant(UTC));
    }

    @Test
    void cronExpressionsCanBeMappedToOtherZone() {
        // always use next hour
        int hour = now().getHour() + 1;
        hour = hour >= 24 ? 0 : hour;

        Instant now = Instant.now();

        Instant actualNextInstant = CronExpression.create(Cron.daily(hour)).next(Instant.now(), systemDefault());

        Instant expectedNextInstant = OffsetDateTime.ofInstant(now, UTC)
                .withHour(now().withHour(hour).atZone(systemDefault()).withZoneSameInstant(UTC).getHour())
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant();

        assertThat(actualNextInstant).isEqualTo(expectedNextInstant);
    }

    @Test
    void cronExpressionsCanBeMappedToOtherZonePart2() {
        Instant actualNextInstant = CronExpression.create(Cron.hourly()).next(Instant.now(), systemDefault());

        Instant expectedNextInstant = now().plusHours(1).withMinute(0).withSecond(0).withNano(0).atZone(systemDefault()).toInstant();

        assertThat(actualNextInstant).isEqualTo(expectedNextInstant);
    }

    @Test
    void cronExpressionsCanBeMappedToOtherZonePart3() {
        Instant actualNextInstant = CronExpression.create(Cron.minutely()).next(Instant.now(), UTC);

        Instant expectedNextInstant = now().plusMinutes(1).withSecond(0).withNano(0).atZone(systemDefault()).toInstant();

        assertThat(actualNextInstant).isEqualTo(expectedNextInstant);
    }

    @Test
    void cronExpressionsAreEqual() {
        CronExpression cronExpression1 = CronExpression.create(Cron.minutely());
        CronExpression cronExpression2 = CronExpression.create(Cron.minutely());

        assertThat(cronExpression1)
                .isEqualTo(cronExpression2)
                .hasSameHashCodeAs(cronExpression2);
    }

    @Test
    void cronExpressionCanBeCompared() {
        Instant now = Instant.now();

        CronExpression cronExpression1 = CronExpression.create(Cron.daily(23, 58));
        CronExpression cronExpression2 = CronExpression.create(Cron.daily(23, 59));

        assertThat(cronExpression1)
                .describedAs("Expecting %s to be less than %s. Current LocalDateTime", cronExpression1.next(now, UTC).toString(), cronExpression2.next(now, UTC).toString(), now.toString())
                .isLessThan(cronExpression2);
    }

    @Test
    void invalidCronExpressionThrowsException() {
        assertThatThrownBy(() -> CronExpression.create("invalid")).isInstanceOf(InvalidCronExpressionException.class);
    }

    @Test
    void invalidCronExpressionThrowsExceptionIfBothLastDayOfMonth() {
        assertThatThrownBy(() -> CronExpression.create("0 0 0 l * 5L"))
                .isInstanceOf(InvalidCronExpressionException.class)
                .hasMessage("You can only specify the last day of month week in either the DAY field or in the DAY_OF_WEEK field, not both.");
    }

    static Stream<Arguments> startInstantCronExpressionAndResultInstant() {
        return Stream.of(
                arguments("* * * * * *", "2019-01-01 00:00:00", "2019-01-01 00:00:01"),
                arguments("* * * * * *", "2019-01-01 00:00:09", "2019-01-01 00:00:10"),
                arguments("* * * * * *", "2019-01-01 00:00:29", "2019-01-01 00:00:30"),
                arguments("* * * * * *", "2019-01-01 00:00:37", "2019-01-01 00:00:38"),
                // Second rollover
                arguments("* * * * * *", "2019-01-01 00:58:59", "2019-01-01 00:59:00"),
                arguments("* * * * * *", "2019-01-01 11:59:59", "2019-01-01 12:00:00"),
                // Minute rollover
                arguments("* * * * * *", "2019-01-01 00:59:59", "2019-01-01 01:00:00"),
                arguments("* * * * * *", "2019-01-01 11:59:59", "2019-01-01 12:00:00"),
                // Hour rollover
                arguments("* * * * * *", "2019-01-01 23:59:59", "2019-01-02 00:00:00"),
                // Month rollover
                arguments("* * * * * *", "2019-01-01 23:59:59", "2019-01-02 00:00:00"),
                arguments("* * * * * *", "2019-02-28 23:59:59", "2019-03-01 00:00:00"),
                // Year rollover
                arguments("* * * * * *", "2019-12-31 23:59:59", "2020-01-01 00:00:00"),
                // Leap year
                arguments("* * * * * *", "2020-02-28 23:59:59", "2020-02-29 00:00:00"),

                // Minute resolution all asterisks
                arguments("* * * * *", "2019-01-01 00:00:00", "2019-01-01 00:01:00"),
                arguments("* * * * *", "2019-01-01 00:09:00", "2019-01-01 00:10:00"),
                arguments("* * * * *", "2019-01-01 00:29:00", "2019-01-01 00:30:00"),
                arguments("* * * * *", "2019-01-01 00:37:00", "2019-01-01 00:38:00"),
                // Minute rollover
                arguments("* * * * *", "2019-01-01 00:59:00", "2019-01-01 01:00:00"),
                arguments("* * * * *", "2019-01-01 11:59:00", "2019-01-01 12:00:00"),
                // Hour rollover
                arguments("* * * * *", "2019-01-01 23:59:00", "2019-01-02 00:00:00"),
                // Month rollover
                arguments("* * * * *", "2019-01-01 23:59:00", "2019-01-02 00:00:00"),
                arguments("* * * * *", "2019-02-28 23:59:00", "2019-03-01 00:00:00"),
                // Year rollover
                arguments("* * * * *", "2019-12-31 23:59:00", "2020-01-01 00:00:00"),
                // Leap year
                arguments("* * * * *", "2020-02-28 23:59:00", "2020-02-29 00:00:00"),

                // Second tests

                // Restricted second
                arguments("44 * * * * *", "2019-01-01 00:00:00", "2019-01-01 00:00:44"),
                arguments("44 * * * * *", "2019-01-01 00:00:44", "2019-01-01 00:01:44"),
                arguments("44 * * * * *", "2019-01-01 00:00:45", "2019-01-01 00:01:44"),

                // test seconds range
                arguments("0-20 * * * * *", "2019-01-01 00:00:00", "2019-01-01 00:00:01"),
                arguments("0-20 * * * * *", "2019-01-01 00:00:19", "2019-01-01 00:00:20"),
                arguments("0-20 * * * * *", "2019-01-01 00:00:14", "2019-01-01 00:00:15"),
                arguments("0-20 * * * * *", "2019-01-01 00:00:20", "2019-01-01 00:01:00"),
                arguments("0-20 * * * * *", "2019-01-01 00:00:59", "2019-01-01 00:01:00"),

                // test seconds list
                arguments("1,5,20,59 * * * * *", "2019-01-01 00:00:00", "2019-01-01 00:00:01"),
                arguments("1,5,20,59 * * * * *", "2019-01-01 00:00:01", "2019-01-01 00:00:05"),
                arguments("1,5,20,59 * * * * *", "2019-01-01 00:00:05", "2019-01-01 00:00:20"),
                arguments("1,5,20,59 * * * * *", "2019-01-01 00:00:58", "2019-01-01 00:00:59"),
                arguments("1,5,20,59 * * * * *", "2019-01-01 00:00:59", "2019-01-01 00:01:01"),

                // test seconds step/every
                arguments("*/3 * * * * *", "2019-01-01 00:00:00", "2019-01-01 00:00:03"),
                arguments("*/3 * * * * *", "2019-01-01 00:00:02", "2019-01-01 00:00:03"),
                arguments("*/3 * * * * *", "2019-01-01 00:00:03", "2019-01-01 00:00:06"),
                arguments("*/3 * * * * *", "2019-01-01 00:00:30", "2019-01-01 00:00:33"),
                arguments("*/3 * * * * *", "2019-01-01 00:00:44", "2019-01-01 00:00:45"),
                arguments("*/3 * * * * *", "2019-01-01 00:00:58", "2019-01-01 00:01:00"),
                arguments("*/3 * * * * *", "2019-01-01 00:00:59", "2019-01-01 00:01:00"),

                arguments("40/3 * * * * *", "2019-01-01 00:00:00", "2019-01-01 00:00:40"),
                arguments("40/3 * * * * *", "2019-01-01 00:00:40", "2019-01-01 00:00:43"),
                arguments("40/3 * * * * *", "2019-01-01 00:00:47", "2019-01-01 00:00:49"),
                arguments("40/3 * * * * *", "2019-01-01 00:00:49", "2019-01-01 00:00:52"),
                arguments("40/3 * * * * *", "2019-01-01 00:00:52", "2019-01-01 00:00:55"),
                arguments("40/3 * * * * *", "2019-01-01 00:00:58", "2019-01-01 00:01:40"),
                arguments("40/3 * * * * *", "2019-01-01 00:00:59", "2019-01-01 00:01:40"),

                arguments("40-58/3 * * * * *", "2019-01-01 00:00:00", "2019-01-01 00:00:40"),
                arguments("40-58/3 * * * * *", "2019-01-01 00:00:40", "2019-01-01 00:00:43"),
                arguments("40-58/3 * * * * *", "2019-01-01 00:00:47", "2019-01-01 00:00:49"),
                arguments("40-58/3 * * * * *", "2019-01-01 00:00:49", "2019-01-01 00:00:52"),
                arguments("40-58/3 * * * * *", "2019-01-01 00:00:52", "2019-01-01 00:00:55"),
                arguments("40-58/3 * * * * *", "2019-01-01 00:00:58", "2019-01-01 00:01:40"),
                arguments("40-58/3 * * * * *", "2019-01-01 00:00:59", "2019-01-01 00:01:40"),

                // test seconds range and list
                arguments("1,0-20,5,30,20,59 * * * * *", "2019-01-01 00:00:00", "2019-01-01 00:00:01"),
                arguments("1,0-20,5,30,20,59 * * * * *", "2019-01-01 00:00:01", "2019-01-01 00:00:02"),
                arguments("1,0-20,5,30,20,59 * * * * *", "2019-01-01 00:00:05", "2019-01-01 00:00:06"),
                arguments("1,0-20,5,30,20,59 * * * * *", "2019-01-01 00:00:25", "2019-01-01 00:00:30"),
                arguments("1,0-20,5,30,20,59 * * * * *", "2019-01-01 00:00:58", "2019-01-01 00:00:59"),
                arguments("1,0-20,5,30,20,59 * * * * *", "2019-01-01 00:00:59", "2019-01-01 00:01:00"),

                // test seconds range ,list and step
                arguments("1,0-20,5,30,20,59,15-21/3 * * * * *", "2019-01-01 00:00:00", "2019-01-01 00:00:01"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * * *", "2019-01-01 00:00:01", "2019-01-01 00:00:02"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * * *", "2019-01-01 00:00:05", "2019-01-01 00:00:06"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * * *", "2019-01-01 00:00:14", "2019-01-01 00:00:15"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * * *", "2019-01-01 00:00:25", "2019-01-01 00:00:30"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * * *", "2019-01-01 00:00:58", "2019-01-01 00:00:59"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * * *", "2019-01-01 00:00:59", "2019-01-01 00:01:00"),

                // Minutes tests

                // Restricted minute
                arguments("44 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:44:00"),

                // test minutes range
                arguments("0-20 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:01:00"),
                arguments("0-20 * * * *", "2019-01-01 00:19:00", "2019-01-01 00:20:00"),
                arguments("0-20 * * * *", "2019-01-01 00:14:00", "2019-01-01 00:15:00"),
                arguments("0-20 * * * *", "2019-01-01 00:20:00", "2019-01-01 01:00:00"),
                arguments("0-20 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:00:00"),

                // test minutes list
                arguments("1,5,20,59 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:01:00"),
                arguments("1,5,20,59 * * * *", "2019-01-01 00:01:00", "2019-01-01 00:05:00"),
                arguments("1,5,20,59 * * * *", "2019-01-01 00:05:00", "2019-01-01 00:20:00"),
                arguments("1,5,20,59 * * * *", "2019-01-01 00:58:00", "2019-01-01 00:59:00"),
                arguments("1,5,20,59 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:01:00"),

                // test minutes step/every
                arguments("*/3 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:03:00"),
                arguments("*/3 * * * *", "2019-01-01 00:02:00", "2019-01-01 00:03:00"),
                arguments("*/3 * * * *", "2019-01-01 00:03:00", "2019-01-01 00:06:00"),
                arguments("*/3 * * * *", "2019-01-01 00:30:00", "2019-01-01 00:33:00"),
                arguments("*/3 * * * *", "2019-01-01 00:44:00", "2019-01-01 00:45:00"),
                arguments("*/3 * * * *", "2019-01-01 00:58:00", "2019-01-01 01:00:00"),
                arguments("*/3 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:00:00"),

                arguments("40/3 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:40:00"),
                arguments("40/3 * * * *", "2019-01-01 00:40:00", "2019-01-01 00:43:00"),
                arguments("40/3 * * * *", "2019-01-01 00:47:00", "2019-01-01 00:49:00"),
                arguments("40/3 * * * *", "2019-01-01 00:49:00", "2019-01-01 00:52:00"),
                arguments("40/3 * * * *", "2019-01-01 00:52:00", "2019-01-01 00:55:00"),
                arguments("40/3 * * * *", "2019-01-01 00:58:00", "2019-01-01 01:40:00"),
                arguments("40/3 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:40:00"),

                arguments("40-58/3 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:40:00"),
                arguments("40-58/3 * * * *", "2019-01-01 00:40:00", "2019-01-01 00:43:00"),
                arguments("40-58/3 * * * *", "2019-01-01 00:47:00", "2019-01-01 00:49:00"),
                arguments("40-58/3 * * * *", "2019-01-01 00:49:00", "2019-01-01 00:52:00"),
                arguments("40-58/3 * * * *", "2019-01-01 00:52:00", "2019-01-01 00:55:00"),
                arguments("40-58/3 * * * *", "2019-01-01 00:58:00", "2019-01-01 01:40:00"),
                arguments("40-58/3 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:40:00"),

                // test minutes range and list
                arguments("1,0-20,5,30,20,59 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:01:00"),
                arguments("1,0-20,5,30,20,59 * * * *", "2019-01-01 00:01:00", "2019-01-01 00:02:00"),
                arguments("1,0-20,5,30,20,59 * * * *", "2019-01-01 00:05:00", "2019-01-01 00:06:00"),
                arguments("1,0-20,5,30,20,59 * * * *", "2019-01-01 00:25:00", "2019-01-01 00:30:00"),
                arguments("1,0-20,5,30,20,59 * * * *", "2019-01-01 00:58:00", "2019-01-01 00:59:00"),
                arguments("1,0-20,5,30,20,59 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:00:00"),

                // test minutes range ,list and step
                arguments("1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:01:00"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:01:00", "2019-01-01 00:02:00"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:05:00", "2019-01-01 00:06:00"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:14:00", "2019-01-01 00:15:00"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:25:00", "2019-01-01 00:30:00"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:58:00", "2019-01-01 00:59:00"),
                arguments("1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:00:00"),

                // Minutes tests - seconds resolution

                // Restricted minute - seconds resolution
                arguments("0 44 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:44:00"),
                arguments("20 44 * * * *", "2019-01-01 00:44:05", "2019-01-01 00:44:20"),
                arguments("20 44 * * * *", "2019-01-01 00:44:21", "2019-01-01 01:44:20"),
                arguments("20 44 * * * *", "2019-01-01 00:45:05", "2019-01-01 01:44:20"),

                // test minutes range - seconds resolution
                arguments("0 0-20 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:01:00"),
                arguments("0 0-20 * * * *", "2019-01-01 00:19:00", "2019-01-01 00:20:00"),
                arguments("0 0-20 * * * *", "2019-01-01 00:14:00", "2019-01-01 00:15:00"),
                arguments("0 0-20 * * * *", "2019-01-01 00:20:00", "2019-01-01 01:00:00"),
                arguments("0 0-20 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:00:00"),

                // test minutes list - seconds resolution
                arguments("0 1,5,20,59 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:01:00"),
                arguments("0 1,5,20,59 * * * *", "2019-01-01 00:01:02", "2019-01-01 00:05:00"),
                arguments("0 1,5,20,59 * * * *", "2019-01-01 00:05:05", "2019-01-01 00:20:00"),
                arguments("0 1,5,20,59 * * * *", "2019-01-01 00:58:00", "2019-01-01 00:59:00"),
                arguments("0 1,5,20,59 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:01:00"),

                // test minutes step/every - seconds resolution
                arguments("0 */3 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:03:00"),
                arguments("0 */3 * * * *", "2019-01-01 00:02:00", "2019-01-01 00:03:00"),
                arguments("0 */3 * * * *", "2019-01-01 00:03:00", "2019-01-01 00:06:00"),
                arguments("0 */3 * * * *", "2019-01-01 00:30:02", "2019-01-01 00:33:00"),
                arguments("0 */3 * * * *", "2019-01-01 00:44:00", "2019-01-01 00:45:00"),
                arguments("0 */3 * * * *", "2019-01-01 00:58:00", "2019-01-01 01:00:00"),
                arguments("0 */3 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:00:00"),

                arguments("0 40/3 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:40:00"),
                arguments("0 40/3 * * * *", "2019-01-01 00:40:00", "2019-01-01 00:43:00"),
                arguments("0 40/3 * * * *", "2019-01-01 00:47:00", "2019-01-01 00:49:00"),
                arguments("0 40/3 * * * *", "2019-01-01 00:49:00", "2019-01-01 00:52:00"),
                arguments("0 40/3 * * * *", "2019-01-01 00:52:00", "2019-01-01 00:55:00"),
                arguments("0 40/3 * * * *", "2019-01-01 00:58:00", "2019-01-01 01:40:00"),
                arguments("0 40/3 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:40:00"),

                arguments("0 40-58/3 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:40:00"),
                arguments("0 40-58/3 * * * *", "2019-01-01 00:40:00", "2019-01-01 00:43:00"),
                arguments("0 40-58/3 * * * *", "2019-01-01 00:47:00", "2019-01-01 00:49:00"),
                arguments("0 40-58/3 * * * *", "2019-01-01 00:49:00", "2019-01-01 00:52:00"),
                arguments("0 40-58/3 * * * *", "2019-01-01 00:52:00", "2019-01-01 00:55:00"),
                arguments("0 40-58/3 * * * *", "2019-01-01 00:58:00", "2019-01-01 01:40:00"),
                arguments("0 40-58/3 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:40:00"),

                // test minutes range and list - seconds resolution
                arguments("0 1,0-20,5,30,20,59 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:01:00"),
                arguments("0 1,0-20,5,30,20,59 * * * *", "2019-01-01 00:01:00", "2019-01-01 00:02:00"),
                arguments("0 1,0-20,5,30,20,59 * * * *", "2019-01-01 00:05:00", "2019-01-01 00:06:00"),
                arguments("0 1,0-20,5,30,20,59 * * * *", "2019-01-01 00:25:00", "2019-01-01 00:30:00"),
                arguments("0 1,0-20,5,30,20,59 * * * *", "2019-01-01 00:58:00", "2019-01-01 00:59:00"),
                arguments("0 1,0-20,5,30,20,59 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:00:00"),

                // test minutes range ,list and step - seconds resolution
                arguments("0 1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:00:00", "2019-01-01 00:01:00"),
                arguments("0 1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:01:00", "2019-01-01 00:02:00"),
                arguments("0 1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:05:00", "2019-01-01 00:06:00"),
                arguments("0 1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:14:00", "2019-01-01 00:15:00"),
                arguments("0 1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:25:00", "2019-01-01 00:30:00"),
                arguments("0 1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:58:00", "2019-01-01 00:59:00"),
                arguments("0 1,0-20,5,30,20,59,15-21/3 * * * *", "2019-01-01 00:59:00", "2019-01-01 01:00:00"),

                // Hours tests

                // Restricted hour
                arguments("* * 14 * * *", "2019-01-01 00:00:00", "2019-01-01 14:00:00"),
                arguments("* * 13 * * *", "2019-01-01 00:09:00", "2019-01-01 13:00:00"),
                arguments("2 39 5 * * *", "2019-01-04 02:38:00", "2019-01-04 05:39:02"),
                arguments("2 39 5 * * *", "2019-01-04 05:38:00", "2019-01-04 05:39:02"),
                arguments("2 39 5 * * *", "2019-01-04 06:38:00", "2019-01-05 05:39:02"),
                arguments("2 39 5 * * *", "2019-01-04 02:40:00", "2019-01-04 05:39:02"),
                arguments("2 39 5 * * *", "2019-01-04 05:40:00", "2019-01-05 05:39:02"),
                arguments("2 39 5 * * *", "2019-01-04 06:40:00", "2019-01-05 05:39:02"),

                // test hours range
                arguments("0 0 0-20 * * *", "2019-01-01 00:02:05", "2019-01-01 01:00:00"),
                arguments("0 0 0-20 * * *", "2019-01-01 19:08:06", "2019-01-01 20:00:00"),
                arguments("0 0 0-20 * * *", "2019-01-01 14:15:15", "2019-01-01 15:00:00"),
                arguments("0 0 0-20 * * *", "2019-01-01 20:22:04", "2019-01-02 00:00:00"),
                arguments("0 0 0-20 * * *", "2019-01-01 23:05:11", "2019-01-02 00:00:00"),

                // test hours list
                arguments("0 0 1,5,20 * * *", "2019-01-01 00:03:00", "2019-01-01 01:00:00"),
                arguments("0 0 1,5,20 * * *", "2019-01-01 01:00:04", "2019-01-01 05:00:00"),
                arguments("0 0 1,5,20 * * *", "2019-01-01 05:08:40", "2019-01-01 20:00:00"),
                arguments("0 0 1,5,20 * * *", "2019-01-01 22:00:09", "2019-01-02 01:00:00"),
                arguments("0 0 1,5,20 * * *", "2019-01-02 23:09:00", "2019-01-03 01:00:00"),

                // test hours step/every
                arguments("0 0 */3 * * *", "2019-01-01 00:03:01", "2019-01-01 03:00:00"),
                arguments("0 0 */3 * * *", "2019-01-01 02:00:00", "2019-01-01 03:00:00"),
                arguments("0 0 */3 * * *", "2019-01-01 03:01:00", "2019-01-01 06:00:00"),
                arguments("0 0 */3 * * *", "2019-01-01 14:00:00", "2019-01-01 15:00:00"),
                arguments("0 0 */3 * * *", "2019-01-01 21:00:00", "2019-01-02 00:00:00"),
                arguments("0 0 */3 * * *", "2019-01-01 23:00:00", "2019-01-02 00:00:00"),

                arguments("0 0 12/3 * * *", "2019-01-01 00:00:00", "2019-01-01 12:00:00"),
                arguments("0 0 12/3 * * *", "2019-01-01 12:00:00", "2019-01-01 15:00:00"),
                arguments("0 0 12/3 * * *", "2019-01-01 19:00:00", "2019-01-01 21:00:00"),
                arguments("0 0 12/3 * * *", "2019-01-01 22:00:00", "2019-01-02 12:00:00"),
                arguments("0 0 12/3 * * *", "2019-01-01 23:00:00", "2019-01-02 12:00:00"),

                arguments("0 0 12-23/3 * * *", "2019-01-01 00:00:00", "2019-01-01 12:00:00"),
                arguments("0 0 12-23/3 * * *", "2019-01-01 12:00:00", "2019-01-01 15:00:00"),
                arguments("0 0 12-23/3 * * *", "2019-01-01 19:00:00", "2019-01-01 21:00:00"),
                arguments("0 0 12-23/3 * * *", "2019-01-01 22:00:00", "2019-01-02 12:00:00"),
                arguments("0 0 12-23/3 * * *", "2019-01-01 23:00:00", "2019-01-02 12:00:00"),

                arguments("0 0 8/1 * * *", "2019-01-01 06:00:00", "2019-01-01 08:00:00"),
                arguments("0 0 8/1 * * *", "2019-01-01 08:00:00", "2019-01-01 09:00:00"),
                arguments("0 0 8-23/1 * * *", "2019-01-01 06:00:00", "2019-01-01 08:00:00"),
                arguments("0 0 8-23/1 * * *", "2019-01-01 08:00:00", "2019-01-01 09:00:00"),

                // test hours range and list
                arguments("0 0 1,0-9,5,23 * * *", "2019-01-01 00:00:00", "2019-01-01 01:00:00"),
                arguments("0 0 1,0-9,5,23 * * *", "2019-01-01 01:00:00", "2019-01-01 02:00:00"),
                arguments("0 0 1,0-9,5,23 * * *", "2019-01-01 05:00:00", "2019-01-01 06:00:00"),
                arguments("0 0 1,0-9,5,23 * * *", "2019-01-01 09:00:00", "2019-01-01 23:00:00"),
                arguments("0 0 1,0-9,5,23 * * *", "2019-01-01 23:00:00", "2019-01-02 00:00:00"),

                // test hours range ,list and step
                arguments("0 0 1,0-9,5,23,15-21/3 * * *", "2019-01-01 00:00:00", "2019-01-01 01:00:00"),
                arguments("0 0 1,0-9,5,23,15-21/3 * * *", "2019-01-01 01:00:00", "2019-01-01 02:00:00"),
                arguments("0 0 1,0-9,5,23,15-21/3 * * *", "2019-01-01 05:00:00", "2019-01-01 06:00:00"),
                arguments("0 0 1,0-9,5,23,15-21/3 * * *", "2019-01-01 14:00:00", "2019-01-01 15:00:00"),
                arguments("0 0 1,0-9,5,23,15-21/3 * * *", "2019-01-01 19:00:00", "2019-01-01 21:00:00"),
                arguments("0 0 1,0-9,5,23,15-21/3 * * *", "2019-01-01 22:00:00", "2019-01-01 23:00:00"),
                arguments("0 0 1,0-9,5,23,15-21/3 * * *", "2019-01-01 23:00:00", "2019-01-02 00:00:00"),

                // days tests

                // restricted day
                arguments("* * 8 14 * *", "2019-01-01 00:00:00", "2019-01-14 08:00:00"),
                arguments("* * 8 13 * *", "2019-01-01 00:09:00", "2019-01-13 08:00:00"),
                arguments("2 39 8 5 * *", "2019-01-03 02:38:00", "2019-01-05 08:39:02"),
                arguments("2 39 8 5 * *", "2019-01-04 05:38:00", "2019-01-05 08:39:02"),
                arguments("2 39 8 5 * *", "2019-01-05 06:38:00", "2019-01-05 08:39:02"),
                arguments("2 39 8 5 * *", "2019-01-06 02:40:00", "2019-02-05 08:39:02"),
                arguments("2 39 8 5 * *", "2019-01-12 05:40:00", "2019-02-05 08:39:02"),
                arguments("2 39 8 5 * *", "2019-01-11 06:40:00", "2019-02-05 08:39:02"),

                // test days range
                arguments("0 0 0 1-20 * *", "2019-01-01 00:02:05", "2019-01-02 00:00:00"),
                arguments("0 0 0 1-20 * *", "2019-01-07 19:08:06", "2019-01-08 00:00:00"),
                arguments("0 0 0 1-20 * *", "2019-01-19 14:15:15", "2019-01-20 00:00:00"),
                arguments("0 0 0 1-20 * *", "2019-01-20 20:22:04", "2019-02-01 00:00:00"),
                arguments("0 0 0 1-20 * *", "2019-01-22 23:05:11", "2019-02-01 00:00:00"),

                // test days list
                arguments("0 0 0 1,5,20 * *", "2019-01-01 00:03:00", "2019-01-05 00:00:00"),
                arguments("0 0 0 1,5,20 * *", "2019-01-05 01:00:04", "2019-01-20 00:00:00"),
                arguments("0 0 0 1,5,20 * *", "2019-01-07 05:08:40", "2019-01-20 00:00:00"),
                arguments("0 0 0 1,5,20 * *", "2019-01-19 22:00:09", "2019-01-20 00:00:00"),
                arguments("0 0 0 1,5,20 * *", "2019-01-20 23:09:00", "2019-02-01 00:00:00"),
                arguments("0 0 0 1,5,20 * *", "2019-01-21 23:09:00", "2019-02-01 00:00:00"),

                // test days step/every
                arguments("0 0 0 */3 * *", "2019-01-01 00:03:01", "2019-01-04 00:00:00"),
                arguments("0 0 0 */3 * *", "2019-01-03 02:00:00", "2019-01-04 00:00:00"),
                arguments("0 0 0 */3 * *", "2019-01-05 03:01:00", "2019-01-07 00:00:00"),
                arguments("0 0 0 */3 * *", "2019-01-10 14:00:00", "2019-01-13 00:00:00"),
                arguments("0 0 0 */3 * *", "2019-01-30 21:00:00", "2019-01-31 00:00:00"),
                arguments("0 0 0 */3 * *", "2019-01-31 23:00:00", "2019-02-01 00:00:00"),

                arguments("0 0 0 12/3 * *", "2019-01-01 00:00:00", "2019-01-12 00:00:00"),
                arguments("0 0 0 12/3 * *", "2019-01-04 12:00:00", "2019-01-12 00:00:00"),
                arguments("0 0 0 12/3 * *", "2019-01-12 19:00:00", "2019-01-15 00:00:00"),
                arguments("0 0 0 12/3 * *", "2019-01-15 22:00:00", "2019-01-18 00:00:00"),
                arguments("0 0 0 12/3 * *", "2019-01-30 23:00:00", "2019-02-12 00:00:00"),

                arguments("0 0 0 12-23/3 * *", "2019-01-01 00:00:00", "2019-01-12 00:00:00"),
                arguments("0 0 0 12-23/3 * *", "2019-01-04 12:00:00", "2019-01-12 00:00:00"),
                arguments("0 0 0 12-23/3 * *", "2019-01-12 19:00:00", "2019-01-15 00:00:00"),
                arguments("0 0 0 12-23/3 * *", "2019-01-15 22:00:00", "2019-01-18 00:00:00"),
                arguments("0 0 0 12-23/3 * *", "2019-01-23 22:00:00", "2019-02-12 00:00:00"),
                arguments("0 0 0 12-23/3 * *", "2019-01-30 23:00:00", "2019-02-12 00:00:00"),

                // test days range and list
                arguments("0 0 0 1,1-9,5,23 * *", "2019-01-01 00:00:00", "2019-01-02 00:00:00"),
                arguments("0 0 0 1,1-9,5,23 * *", "2019-01-04 01:00:00", "2019-01-05 00:00:00"),
                arguments("0 0 0 1,1-9,5,23 * *", "2019-01-22 05:00:00", "2019-01-23 00:00:00"),
                arguments("0 0 0 1,1-9,5,23 * *", "2019-01-23 09:00:00", "2019-02-01 00:00:00"),
                arguments("0 0 0 1,1-9,5,23 * *", "2019-01-31 23:00:00", "2019-02-01 00:00:00"),

                // test days range ,list and step
                arguments("0 0 0 1,1-9,5,23,15-21/3 * *", "2019-01-01 00:00:00", "2019-01-02 00:00:00"),
                arguments("0 0 0 1,1-9,5,23,15-21/3 * *", "2019-01-05 01:00:00", "2019-01-06 00:00:00"),
                arguments("0 0 0 1,1-9,5,23,15-21/3 * *", "2019-01-09 05:00:00", "2019-01-15 00:00:00"),
                arguments("0 0 0 1,1-9,5,23,15-21/3 * *", "2019-01-13 14:00:00", "2019-01-15 00:00:00"),
                arguments("0 0 0 1,1-9,5,23,15-21/3 * *", "2019-01-15 19:00:00", "2019-01-18 00:00:00"),
                arguments("0 0 0 1,1-9,5,23,15-21/3 * *", "2019-01-21 22:00:00", "2019-01-23 00:00:00"),
                arguments("0 0 0 1,1-9,5,23,15-21/3 * *", "2019-01-28 23:00:00", "2019-02-01 00:00:00"),

                // Day of month tests
                arguments("30  *  1 * *", "2019-01-07 00:00:00", "2019-02-01 00:30:00"),
                arguments("30  *  1 * *", "2019-02-01 00:30:00", "2019-02-01 01:30:00"),

                arguments("10  * 22    * *", "2019-01-01 00:00:00", "2019-01-22 00:10:00"),
                arguments("30 23 19    * *", "2019-01-01 00:00:00", "2019-01-19 23:30:00"),
                arguments("30 23 21    * *", "2019-01-01 00:00:00", "2019-01-21 23:30:00"),
                arguments(" *  * 21    * *", "2019-01-01 00:01:00", "2019-01-21 00:00:00"),
                arguments(" *  * 30,31 * *", "2019-07-10 00:00:00", "2019-07-30 00:00:00"),

                // test months

                // Restricted month
                arguments("* * * * Apr *", "2019-01-01 00:00:00", "2019-04-01 00:00:00"),
                arguments("* * * * 4 *", "2019-01-01 00:00:00", "2019-04-01 00:00:00"),
                arguments("* * * * 2 *", "2019-01-01 00:09:00", "2019-02-01 00:00:00"),
                arguments("0 39 5 3 7 *", "2019-01-04 02:40:00", "2019-07-03 05:39:00"),
                arguments("* * * * 1 *", "2019-12-01 00:00:00", "2020-01-01 00:00:00"),
                arguments("0 0 30 1 *", "2019-12-01 00:00:00", "2020-01-30 00:00:00"),
                arguments("0 0 28 2 *", "2019-12-01 00:00:00", "2020-02-28 00:00:00"),
                arguments("* * 31 12 *", "2019-12-01 00:00:00", "2019-12-31 00:00:00"),
                arguments("* 0 0 30 1 *", "2019-12-01 00:00:00", "2020-01-30 00:00:00"),
                arguments("0 0 31 12 *", "2019-12-01 00:00:00", "2019-12-31 00:00:00"),
                arguments("0 0 30 12 *", "2019-12-01 00:00:00", "2019-12-30 00:00:00"),

                // test months range
                arguments("0 0 1 1-8 *", "2019-01-01 00:02:05", "2019-02-01 00:00:00"),
                arguments("0 0 1 1-8 *", "2019-06-01 19:08:06", "2019-07-01 00:00:00"),
                arguments("0 0 1 1-8 *", "2019-07-01 14:15:15", "2019-08-01 00:00:00"),
                arguments("0 0 1 1-8 *", "2019-08-01 20:22:04", "2020-01-01 00:00:00"),
                arguments("0 0 1 1-8 *", "2019-10-01 23:05:11", "2020-01-01 00:00:00"),

                // test months list
                arguments("0 0 1 Jan,May,August *", "2019-01-01 00:03:00", "2019-05-01 00:00:00"),
                arguments("0 0 1 1,5,8 *", "2019-02-01 01:00:04", "2019-05-01 00:00:00"),
                arguments("0 0 1 1,5,8 *", "2019-05-01 05:08:40", "2019-08-01 00:00:00"),
                arguments("0 0 1 1,5,8 *", "2019-08-01 22:00:09", "2020-01-01 00:00:00"),
                arguments("0 0 1 1,5,8 *", "2019-10-02 23:09:00", "2020-01-01 00:00:00"),

                // test months step/every
                arguments("0 0 1 */3 *", "2019-01-01 00:03:01", "2019-04-01 00:00:00"),
                arguments("0 0 1 */3 *", "2019-02-01 02:00:00", "2019-04-01 00:00:00"),
                arguments("0 0 1 */3 *", "2019-04-01 03:01:00", "2019-07-01 00:00:00"),
                arguments("0 0 1 */3 *", "2019-06-01 14:00:00", "2019-07-01 00:00:00"),
                arguments("0 0 1 */3 *", "2019-07-01 14:00:00", "2019-10-01 00:00:00"),
                arguments("0 0 1 */3 *", "2019-09-01 21:00:00", "2019-10-01 00:00:00"),
                arguments("0 0 1 */3 *", "2019-12-01 23:00:00", "2020-01-01 00:00:00"),

                arguments("0 0 1 5/3 *", "2019-01-01 00:00:00", "2019-05-01 00:00:00"),
                arguments("0 0 1 5/3 *", "2019-04-01 12:00:00", "2019-05-01 00:00:00"),
                arguments("0 0 1 5/3 *", "2019-05-01 19:00:00", "2019-08-01 00:00:00"),
                arguments("0 0 1 5/3 *", "2019-07-01 22:00:00", "2019-08-01 00:00:00"),
                arguments("0 0 1 5/3 *", "2019-09-01 23:00:00", "2019-11-01 00:00:00"),
                arguments("0 0 1 5/3 *", "2019-12-01 23:00:00", "2020-05-01 00:00:00"),

                arguments("0 0 1 5-12/3 *", "2019-01-01 00:00:00", "2019-05-01 00:00:00"),
                arguments("0 0 1 5-12/3 *", "2019-04-01 12:00:00", "2019-05-01 00:00:00"),
                arguments("0 0 1 5-12/3 *", "2019-05-01 19:00:00", "2019-08-01 00:00:00"),
                arguments("0 0 1 5-12/3 *", "2019-07-01 22:00:00", "2019-08-01 00:00:00"),
                arguments("0 0 1 5-12/3 *", "2019-09-01 23:00:00", "2019-11-01 00:00:00"),
                arguments("0 0 1 5-12/3 *", "2019-12-01 23:00:00", "2020-05-01 00:00:00"),

                // test months range and list
                arguments("0 0 1 1,1-9,5,12 *", "2019-01-01 00:00:00", "2019-02-01 00:00:00"),
                arguments("0 0 1 1,1-9,5,12 *", "2019-03-01 01:00:00", "2019-04-01 00:00:00"),
                arguments("0 0 1 1,1-9,5,12 *", "2019-04-01 01:00:00", "2019-05-01 00:00:00"),
                arguments("0 0 1 1,1-9,5,12 *", "2019-05-01 05:00:00", "2019-06-01 00:00:00"),
                arguments("0 0 1 1,1-9,5,12 *", "2019-09-01 09:00:00", "2019-12-01 00:00:00"),
                arguments("0 0 1 1,1-9,5,12 *", "2019-12-01 23:00:00", "2020-01-01 00:00:00"),

                // test months range ,list and step
                arguments("0 0 1 1,1-3,5,12,8-12/3 *", "2019-01-01 00:00:00", "2019-02-01 00:00:00"),
                arguments("0 0 1 1,1-3,5,12,8-12/3 *", "2019-02-01 01:00:00", "2019-03-01 00:00:00"),
                arguments("0 0 1 1,1-3,5,12,8-12/3 *", "2019-05-01 05:00:00", "2019-08-01 00:00:00"),
                arguments("0 0 1 1,1-3,5,12,8-12/3 *", "2019-08-01 14:00:00", "2019-11-01 00:00:00"),
                arguments("0 0 1 1,1-3,5,12,8-12/3 *", "2019-09-01 19:00:00", "2019-11-01 00:00:00"),
                arguments("0 0 1 1,1-3,5,12,8-12/3 *", "2019-11-01 22:00:00", "2019-12-01 00:00:00"),
                arguments("0 0 1 1,1-3,5,12,8-12/3 *", "2019-12-01 23:00:00", "2020-01-01 00:00:00"),

                // test January
                arguments("0 0 10,30,31 * *", "2016-01-01 00:00:00", "2016-01-10 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-01-10 00:00:00", "2016-01-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-01-30 00:00:00", "2016-01-31 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-01-31 00:00:00", "2016-02-10 00:00:00"),

                // test February
                arguments("0 0 10,30,31 * *", "2016-02-10 00:00:00", "2016-03-10 00:00:00"),
                arguments("0 0 10,29,30,31 * *", "2016-02-10 00:00:00", "2016-02-29 00:00:00"),
                arguments("0 0 10,29,30,31 * *", "2019-02-10 00:00:00", "2019-03-10 00:00:00"),
                arguments("* * * 3 *", "2019-02-28 23:59:59", "2019-03-01 00:00:00"),
                arguments("* * * 3 *", "2020-02-29 23:59:59", "2020-03-01 00:00:00"),

                // test Mar
                arguments("0 0 10,30,31 * *", "2016-03-10 00:00:00", "2016-03-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-03-30 00:00:00", "2016-03-31 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-03-31 00:00:00", "2016-04-10 00:00:00"),
                arguments("* * * 4 *", "2019-03-31 23:59:59", "2019-04-01 00:00:00"),

                // test April
                arguments("0 0 10,30,31 * *", "2016-04-10 00:00:00", "2016-04-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-04-30 00:00:00", "2016-05-10 00:00:00"),
                arguments("* * * 5 *", "2019-04-30 23:59:59", "2019-05-01 00:00:00"),

                // test May
                arguments("0 0 10,30,31 * *", "2016-05-10 00:00:00", "2016-05-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-05-30 00:00:00", "2016-05-31 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-05-31 00:00:00", "2016-06-10 00:00:00"),

                // test June
                arguments("0 0 10,30,31 * *", "2016-06-10 00:00:00", "2016-06-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-06-30 00:00:00", "2016-07-10 00:00:00"),

                // test July
                arguments("0 0 10,30,31 * *", "2016-07-10 00:00:00", "2016-07-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-07-30 00:00:00", "2016-07-31 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-07-31 00:00:00", "2016-08-10 00:00:00"),

                // test August
                arguments("0 0 10,30,31 * *", "2016-08-10 00:00:00", "2016-08-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-08-30 00:00:00", "2016-08-31 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-08-31 00:00:00", "2016-09-10 00:00:00"),

                // test September
                arguments("0 0 10,30,31 * *", "2016-09-10 00:00:00", "2016-09-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-09-30 00:00:00", "2016-10-10 00:00:00"),

                // test October
                arguments("0 0 10,30,31 * *", "2016-10-10 00:00:00", "2016-10-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-10-30 00:00:00", "2016-10-31 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-10-31 00:00:00", "2016-11-10 00:00:00"),

                // test November
                arguments("0 0 10,30,31 * *", "2016-11-10 00:00:00", "2016-11-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-11-30 00:00:00", "2016-12-10 00:00:00"),

                // test December
                arguments("0 0 10,30,31 * *", "2016-12-10 00:00:00", "2016-12-30 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-12-30 00:00:00", "2016-12-31 00:00:00"),
                arguments("0 0 10,30,31 * *", "2016-12-31 00:00:00", "2017-01-10 00:00:00"),

                // leap year tests
                arguments("0 0 0 29 2 *", "2016-02-29 00:00:00", "2020-02-29 00:00:00"),
                arguments("0 0 0 28 2 *", "2016-02-29 00:00:00", "2017-02-28 00:00:00"),
                // leap year dividable by 100 and 400
                arguments("0 0 0 29 2 *", "2000-01-01 00:00:00", "2000-02-29 00:00:00"),
                arguments("0 0 0 29 2 *", "2000-02-29 00:00:00", "2004-02-29 00:00:00"),

                // more year tests
                arguments("0 0 0 28 2 *", "2016-01-01 00:00:00", "2016-02-28 00:00:00"),
                arguments("0 0 0 28 2 *", "2016-02-28 00:00:00", "2017-02-28 00:00:00"),
                arguments("0 0 0 28 2 *", "2018-02-28 00:00:00", "2019-02-28 00:00:00"),
                arguments("0 0 0 28 2 *", "2019-02-28 00:00:00", "2020-02-28 00:00:00"),
                arguments("0 0 0 28 2 *", "2017-02-28 00:00:00", "2018-02-28 00:00:00"),

                // day of week tests
                arguments("0 0 6 * * Sun", "2019-01-12 00:00:00", "2019-01-13 06:00:00"),
                arguments("0 0 6 * * 1", "2019-01-12 00:00:00", "2019-01-14 06:00:00"),
                arguments("0 0 6 * * 2", "2019-01-12 00:00:00", "2019-01-15 06:00:00"),
                arguments("0 0 12 * * 3", "2019-01-12 00:00:00", "2019-01-16 12:00:00"),
                arguments("0 0 12 * * 4", "2019-01-24 00:00:00", "2019-01-24 12:00:00"),

                arguments("0 2 9 * * 3", "2019-01-01 09:45:00", "2019-01-02 09:02:00"),
                arguments("0 2 9 * * 3", "2019-01-02 09:10:00", "2019-01-09 09:02:00"),
                arguments("0 2 9 * * 3", "2019-01-17 05:11:00", "2019-01-23 09:02:00"),
                arguments("0 2 9 * * 3", "2019-01-11 12:07:00", "2019-01-16 09:02:00"),
                arguments("0 2 9 * * 3", "2019-01-19 13:12:00", "2019-01-23 09:02:00"),
                arguments("0 2 9 * * 3", "2019-01-25 22:11:00", "2019-01-30 09:02:00"),

                arguments("0 * * * * 3", "2019-01-13 00:08:00", "2019-01-16 00:00:00"),
                arguments("0 9 23 2 * 3", "2019-01-14 08:00:00", "2019-01-16 23:09:00"),
                arguments("0 9 23 3 * 3", "2019-01-16 11:39:00", "2019-01-16 23:09:00"),

                // intersect day with day of week
                arguments("0 0 0 * * 3", "2019-01-01 00:00:00", "2019-01-02 00:00:00"),
                arguments("0 0 0 * * 6", "2019-01-01 00:00:00", "2019-01-05 00:00:00"),
                arguments("0 0 0 */5 * 2", "2019-01-01 00:00:00", "2019-02-26 00:00:00"),
                arguments("0 0 0 */5 * */5", "2019-01-01 00:00:00", "2019-01-06 00:00:00"),
                arguments("0 0 0 */5 2 */5", "2019-01-01 00:00:00", "2019-02-01 00:00:00"),
                arguments("0 0 0 29 2 */4", "2019-01-01 00:00:00", "2024-02-29 00:00:00"),
                arguments("0 0 0 29 2 */5", "2019-01-01 00:00:00", "2032-02-29 00:00:00"),

                // union day with day of week
                arguments("0 0 0 4 * 3", "2019-01-01 00:00:00", "2019-01-02 00:00:00"),
                arguments("0 0 0 23 * 6", "2019-01-01 00:00:00", "2019-01-05 00:00:00"),
                arguments("0 0 0 12 * 2", "2019-01-01 00:00:00", "2019-01-08 00:00:00"),
                arguments("0 0 0 5 * 5", "2019-01-01 00:00:00", "2019-01-04 00:00:00"),
                arguments("0 0 0 5 2 5", "2019-01-01 00:00:00", "2019-02-01 00:00:00"),
                arguments("0 0 0 29 2 4", "2019-01-01 00:00:00", "2019-02-07 00:00:00"),
                arguments("0 0 0 29 2 5", "2019-01-01 00:00:00", "2019-02-01 00:00:00"),

                // github issue 31
                arguments("36 9 * * *","2020-09-08 09:40:00",  "2020-09-09 09:36:00"),

                // last day of month
                arguments("0 0 0 * * 4l", "2019-01-01 00:00:00", "2019-01-31 00:00:00"),
                arguments("0 0 0 * * 5l", "2019-01-01 00:00:00", "2019-01-25 00:00:00"),
                arguments("0 0 0 * * 5L", "2019-01-01 00:00:00", "2019-01-25 00:00:00"),
                arguments("0 0 0 * * 6l", "2019-01-01 00:00:00", "2019-01-26 00:00:00"),
                arguments("0 0 0 * * 0L", "2019-01-01 00:00:00", "2019-01-27 00:00:00"),
                arguments("0 0 0 * 2 5L", "2019-01-01 00:00:00", "2019-02-22 00:00:00"),
                arguments("0 0 0 * 9 0l", "2019-01-01 00:00:00", "2019-09-29 00:00:00"),
                arguments("0 0 0 * 5 2L", "2019-01-01 00:00:00", "2019-05-28 00:00:00"),
                arguments("0 0 0 * 2 6L", "2019-01-01 00:00:00", "2019-02-23 00:00:00"),
                arguments("0 0 0 * 4 3L", "2019-01-01 00:00:00", "2019-04-24 00:00:00"),
                arguments("0 0 0 * 4 2L", "2019-01-01 00:00:00", "2019-04-30 00:00:00"),

                arguments("0 0 0 l *  *", "2019-01-01 00:00:00", "2019-01-31 00:00:00"),
                arguments("0 0 0 L *  *", "2019-02-20 00:00:00", "2019-02-28 00:00:00"),
                arguments("0 0 0 l *  *", "2019-03-01 00:00:00", "2019-03-31 00:00:00"),
                arguments("0 0 0 L *  *", "2019-04-01 00:00:00", "2019-04-30 00:00:00"),
                arguments("0 0 0 l *  *", "2019-05-01 00:00:00", "2019-05-31 00:00:00"),
                arguments("0 0 0 L *  *", "2019-06-01 00:00:00", "2019-06-30 00:00:00"),
                arguments("0 0 0 l *  *", "2019-07-01 00:00:00", "2019-07-31 00:00:00"),
                arguments("0 0 0 L *  *", "2019-08-01 00:00:00", "2019-08-31 00:00:00"),
                arguments("0 0 0 l *  *", "2019-09-01 00:00:00", "2019-09-30 00:00:00"),
                arguments("0 0 0 L *  *", "2019-10-01 00:00:00", "2019-10-31 00:00:00"),
                arguments("0 0 0 l *  *", "2019-11-01 00:00:00", "2019-11-30 00:00:00"),
                arguments("0 0 0 L *  *", "2019-12-01 00:00:00", "2019-12-31 00:00:00")
        );
    }

}
