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
                arguments(CarbonAwareCron.daily(Duration.ofHours(1), Duration.ofHours(2)), "0 0 * * * PT1H PT2H"),
                arguments(CarbonAwareCron.daily(3, Duration.ofHours(1), Duration.ofHours(2)), "0 3 * * * PT1H PT2H"),
                arguments(CarbonAwareCron.daily(1, 3, Duration.ofHours(1), Duration.ofHours(2)), "3 1 * * * PT1H PT2H"),
                arguments(CarbonAwareCron.dailyAllowedToRunEarlier(5, 3), "0 5 * * * PT3H PT0S"),
                arguments(CarbonAwareCron.dailyAllowedToRunEarlier(3), "0 0 * * * PT3H PT0S"),
                arguments(CarbonAwareCron.dailyAllowedToRunLater(5, 3), "0 5 * * * PT0S PT3H"),
                arguments(CarbonAwareCron.dailyAllowedToRunLater(3), "0 0 * * * PT0S PT3H"),
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
                arguments(CarbonAwareCron.monthlyAllowedToRunEarlier(3), "0 0 1 * * PT72H PT0S"),
                arguments(CarbonAwareCron.monthlyAllowedToRunLater(2), "0 0 1 * * PT0S PT48H"),
                arguments(CarbonAwareCron.monthly(15, 3, 2, 1), "0 3 15 * * PT48H PT24H"),

                // Yearly
                arguments(CarbonAwareCron.yearly(5, 1, 2), "0 0 1 5 * PT720H PT1464H"),
                arguments(CarbonAwareCron.yearlyAllowedToRunEarlier(5, 1, 2), "0 0 1 5 * PT1464H PT0S"),
                arguments(CarbonAwareCron.yearlyAllowedToRunLater(5, 1, 2), "0 0 1 5 * PT0S PT1464H")
        );
    }
}
