package org.jobrunr.scheduling;

import org.jobrunr.scheduling.carbonaware.CarbonAwareScheduleMargin;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ScheduleTest {

    @Test
    void createScheduleWithoutCarbonAwareSchedule() {
        Schedule schedule = new CronExpression(Cron.daily());

        assertThat(schedule.getExpression()).isEqualTo(Cron.daily());
        assertThat(schedule.isCarbonAware()).isFalse();
    }

    @Test
    void createScheduleWithCarbonAwareSchedule() {
        Schedule schedule = new CronExpression("0 0 * * * [PT3H/PT0S]");
        CarbonAwareScheduleMargin carbonAwareScheduleMargin = schedule.getCarbonAwareScheduleMargin();

        assertThat(schedule.getExpression()).isEqualTo("0 0 * * *");
        assertThat(schedule.isCarbonAware()).isTrue();
        assertThat(carbonAwareScheduleMargin.getMarginBefore()).isEqualTo(ofHours(3));
        assertThat(carbonAwareScheduleMargin.getMarginAfter()).isZero();
    }

    @Test
    void createScheduleWithEmptyOrNullExpressionThrowsIllegalArgumentException() {
        assertThatCode(() -> new CronExpression(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected scheduleWithOptionalCarbonAwareScheduleMargin to be non-null and non-empty.");

        assertThatCode(() -> new CronExpression(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected scheduleWithOptionalCarbonAwareScheduleMargin to be non-null and non-empty.");
    }

    @Test
    void durationBetweenSchedulesWithIntervals() {
        assertThat(new Interval(ofMillis(200)).durationBetweenSchedules()).isEqualTo(ofMillis(200));
        assertThat(new Interval(ofSeconds(1)).durationBetweenSchedules()).isEqualTo(ofSeconds(1));
        assertThat(new Interval(ofSeconds(5)).durationBetweenSchedules()).isEqualTo(ofSeconds(5));
    }

    @ParameterizedTest
    @MethodSource("durationBetweenSchedulesCronExpressions")
    void durationBetweenSchedulesWithCronExpressions(String cron, Duration duration) {
        assertThat(new CronExpression(cron).durationBetweenSchedules()).isEqualTo(duration);
    }

    static Stream<Arguments> durationBetweenSchedulesCronExpressions() {
        return Stream.of(
                arguments("* * * * * *", ofSeconds(1)),
                arguments("*/5 * * * * *", ofSeconds(5)),
                arguments("0 0 * * *", ofDays(1)),
                arguments("0 1,5,20 * * *", ofHours(15)),
                arguments("0 0 1 1-8 *", ofDays(28)),
                arguments("9 23 2 * 3", ofDays(5)),
                arguments("0 0 */5 * 2", ofDays(84)),
                arguments("0 0 29 2 */4", ofDays(7305))
        );
    }

    @Test
    void validateThrowsAnExceptionIfCarbonAwareScheduleMarginIsBiggerThanScheduleInterval() {
        assertThatCode(() -> new CronExpression("* * * * * *").validate()).doesNotThrowAnyException();
        assertThatCode(() -> new CronExpression("* * * * * * [PT0S/PT0S]").validate()).doesNotThrowAnyException();
        assertThatCode(() -> new CronExpression("0 0 * * * [PT0S/PT7H]").validate()).doesNotThrowAnyException();

        assertThatCode(() -> new CronExpression("* * * * * * [PT1M/PT0S]").validate()).isInstanceOf(IllegalStateException.class)
                .hasMessage("The total carbon aware margin must be lower than the duration between each schedule.");
        assertThatCode(() -> new CronExpression("0 0 * * * [PT12H/PT24H]").validate()).isInstanceOf(IllegalStateException.class)
                .hasMessage("The total carbon aware margin must be lower than the duration between each schedule.");
    }
}