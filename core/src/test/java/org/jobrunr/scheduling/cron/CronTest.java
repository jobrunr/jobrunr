package org.jobrunr.scheduling.cron;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.DayOfWeek;
import java.time.Month;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CronTest {

    @ParameterizedTest
    @MethodSource("getCronExpressionsAndTheirValue")
    void testCron(String cronExpression, String expectedResult) {
        assertThat(cronExpression).isEqualTo(expectedResult);
    }

    static Stream<Arguments> getCronExpressionsAndTheirValue() {
        return Stream.of(
                arguments(Cron.daily(), "0 0 * * *"),
                arguments(Cron.daily(3), "0 3 * * *"),
                arguments(Cron.daily(3, 17), "17 3 * * *"),
                arguments(Cron.hourly(), "0 * * * *"),
                arguments(Cron.hourly(9), "9 * * * *"),
                arguments(Cron.minutely(), "* * * * *"),
                arguments(Cron.every15seconds(), "*/15 * * * * *"),
                arguments(Cron.every30seconds(), "*/30 * * * * *"),
                arguments(Cron.every5minutes(), "*/5 * * * *"),
                arguments(Cron.every10minutes(), "*/10 * * * *"),
                arguments(Cron.every15minutes(), "*/15 * * * *"),
                arguments(Cron.everyHalfHour(), "*/30 * * * *"),
                arguments(Cron.weekly(), "0 0 * * 1"),
                arguments(Cron.weekly(DayOfWeek.TUESDAY), "0 0 * * 2"),
                arguments(Cron.weekly(DayOfWeek.TUESDAY, 8), "0 8 * * 2"),
                arguments(Cron.weekly(DayOfWeek.TUESDAY, 8, 15), "15 8 * * 2"),
                arguments(Cron.weekly(DayOfWeek.SUNDAY), "0 0 * * 0"),
                arguments(Cron.weekly(DayOfWeek.SUNDAY, 8), "0 8 * * 0"),
                arguments(Cron.weekly(DayOfWeek.SUNDAY, 8, 15), "15 8 * * 0"),
                arguments(Cron.monthly(), "0 0 1 * *"),
                arguments(Cron.monthly(4), "0 0 4 * *"),
                arguments(Cron.monthly(6, 9), "0 9 6 * *"),
                arguments(Cron.monthly(6, 9, 12), "12 9 6 * *"),
                arguments(Cron.lastDayOfTheMonth(), "0 0 L * *"),
                arguments(Cron.lastDayOfTheMonth(10), "0 10 L * *"),
                arguments(Cron.lastDayOfTheMonth(10, 10), "10 10 L * *"),
                arguments(Cron.yearly(), "0 0 1 1 *"),
                arguments(Cron.yearly(Month.FEBRUARY), "0 0 1 2 *"),
                arguments(Cron.yearly(Month.FEBRUARY, 12), "0 0 12 2 *"),
                arguments(Cron.yearly(Month.FEBRUARY, 12, 7), "0 7 12 2 *"),
                arguments(Cron.yearly(Month.FEBRUARY, 12, 7, 6), "6 7 12 2 *")
        );
    }
}