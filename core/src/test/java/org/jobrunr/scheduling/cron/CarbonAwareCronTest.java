package org.jobrunr.scheduling.cron;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CarbonAwareCronTest {

    @ParameterizedTest
    @MethodSource("getCarbonAwareCronExpressionsAndTheirValue")
    void testCron(CarbonAwareCronExpression cronExpression, String expectedResult) {
        assertThat(cronExpression.toString()).isEqualTo(expectedResult);
    }

    static Stream<Arguments> getCarbonAwareCronExpressionsAndTheirValue() {
        return Stream.of(
                // Daily
                arguments(CarbonAwareCron.daily(1, 2), "0 0 * * * PT1H PT2H"),
                arguments(CarbonAwareCron.daily(15, 3, 6), "0 15 * * * PT3H PT6H"),
                arguments(CarbonAwareCron.daily(Duration.ofHours(1), Duration.ofHours(2)), "0 0 * * * PT1H PT2H"),
                arguments(CarbonAwareCron.daily(3, Duration.ofHours(1), Duration.ofHours(2)), "0 3 * * * PT1H PT2H"),
                arguments(CarbonAwareCron.daily(1, 3, Duration.ofHours(1), Duration.ofHours(2)), "3 1 * * * PT1H PT2H"),
                arguments(CarbonAwareCron.daily(1, 16, 1, 2), "16 1 * * * PT1H PT2H"),
                arguments(CarbonAwareCron.dailyAllowedToRunEarlier(5, 3), "0 5 * * * PT3H PT0S"),
                arguments(CarbonAwareCron.dailyAllowedToRunEarlier(5, 22, 3), "22 5 * * * PT3H PT0S"),
                arguments(CarbonAwareCron.dailyAllowedToRunEarlier(3), "0 0 * * * PT3H PT0S"),
                arguments(CarbonAwareCron.dailyAllowedToRunEarlier(Duration.ofMinutes(180)), "0 0 * * * PT3H PT0S"),
                arguments(CarbonAwareCron.dailyAllowedToRunLater(5, 3), "0 5 * * * PT0S PT3H"),
                arguments(CarbonAwareCron.dailyAllowedToRunLater(5, 12, 3), "12 5 * * * PT0S PT3H"),
                arguments(CarbonAwareCron.dailyAllowedToRunLater(3), "0 0 * * * PT0S PT3H"),
                arguments(CarbonAwareCron.dailyAllowedToRunLater(Duration.ofMinutes(180)), "0 0 * * * PT0S PT3H"),
                arguments(CarbonAwareCron.dailyBefore(10), "0 0 * * * PT0S PT10H"),
                arguments(CarbonAwareCron.dailyAfter(10), "0 10 * * * PT0S PT14H"),
                arguments(CarbonAwareCron.dailyBetween(10, 20), "0 10 * * * PT0S PT10H"),
                // Weekly
                arguments(CarbonAwareCron.weekly(1, Duration.ofHours(1), Duration.ofHours(2)), "0 0 * * 1 PT1H PT2H"),
                arguments(CarbonAwareCron.weeklyAllowedToRunEarlier(DayOfWeek.of(1), 3, 5, 7), "5 3 * * 1 PT168H PT0S"),
                arguments(CarbonAwareCron.weeklyAllowedToRunLater(DayOfWeek.of(1), 3, 5, 7), "5 3 * * 1 PT0S PT168H"),
                arguments(CarbonAwareCron.weekly(1, 2), "0 0 * * 1 PT24H PT48H"),
                arguments(CarbonAwareCron.weekly(DayOfWeek.WEDNESDAY, 1, 2), "0 0 * * 3 PT24H PT48H"),

                // Monthly
                arguments(CarbonAwareCron.monthly(1, 2), "0 0 1 * * PT24H PT48H"),
                arguments(CarbonAwareCron.monthly(3, 1, 2), "0 0 3 * * PT24H PT48H"),
                arguments(CarbonAwareCron.monthly(15, 3, 2, 1), "0 3 15 * * PT48H PT24H"),
                arguments(CarbonAwareCron.monthly(15, Duration.ofHours(2), Duration.ofHours(3)), "0 0 15 * * PT2H PT3H"),
                arguments(CarbonAwareCron.monthlyAllowedToRunEarlier(3), "0 0 1 * * PT72H PT0S"),
                arguments(CarbonAwareCron.monthlyAllowedToRunEarlier(18, 3), "0 0 18 * * PT72H PT0S"),
                arguments(CarbonAwareCron.monthlyAllowedToRunEarlier(18, 12, 3), "0 12 18 * * PT72H PT0S"),
                arguments(CarbonAwareCron.monthlyAllowedToRunEarlier(18, 12, 48, 3), "48 12 18 * * PT72H PT0S"),
                arguments(CarbonAwareCron.monthlyAllowedToRunHoursEarlier(18, 12, 48, 3), "48 12 18 * * PT3H PT0S"),
                arguments(CarbonAwareCron.monthlyAllowedToRunLater(2), "0 0 1 * * PT0S PT48H"),
                arguments(CarbonAwareCron.monthlyAllowedToRunLater(9, 2), "0 0 9 * * PT0S PT48H"),
                arguments(CarbonAwareCron.monthlyAllowedToRunLater(9, 22, 2), "0 22 9 * * PT0S PT48H"),
                arguments(CarbonAwareCron.monthlyAllowedToRunLater(9, 22, 50, 2), "50 22 9 * * PT0S PT48H"),
                arguments(CarbonAwareCron.monthlyAllowedToRunHoursLater(3, 22, 50, 10), "50 22 3 * * PT0S PT10H"),

                // Yearly
                arguments(CarbonAwareCron.yearly(5, 1, 2), "0 0 1 5 * PT720H PT1464H"),
                arguments(CarbonAwareCron.yearlyAllowedToRunEarlier(5, 1, 2), "0 0 1 5 * PT1464H PT0S"),
                arguments(CarbonAwareCron.yearlyAllowedToRunLater(5, 1, 2), "0 0 1 5 * PT0S PT1464H")
        );
    }
}
