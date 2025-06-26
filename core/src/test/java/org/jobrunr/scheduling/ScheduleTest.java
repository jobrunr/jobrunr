package org.jobrunr.scheduling;

import org.jobrunr.scheduling.carbonaware.CarbonAwareScheduleMargin;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static java.time.Duration.ofDays;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
        assertThat(carbonAwareScheduleMargin.getMarginBefore()).isEqualTo(Duration.ofHours(3));
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
    void durationBetweenSchedules() {
        assertThat(new CronExpression("* * * * * *").durationBetweenSchedules()).isEqualTo(ofSeconds(1));
        assertThat(new CronExpression("*/5 * * * * *").durationBetweenSchedules()).isEqualTo(ofSeconds(5));
        assertThat(new CronExpression("0 0 * * *").durationBetweenSchedules()).isEqualTo(ofDays(1));

        assertThat(new Interval(ofMillis(200)).durationBetweenSchedules()).isEqualTo(ofMillis(200));
        assertThat(new Interval(ofSeconds(1)).durationBetweenSchedules()).isEqualTo(ofSeconds(1));
        assertThat(new Interval(ofSeconds(5)).durationBetweenSchedules()).isEqualTo(ofSeconds(5));

    }

    @Test
    void validateThrowsAnExceptionIfCarbonAwareScheduleMarginIsBiggerThanScheduleInterval() {
        assertThatCode(() -> new CronExpression("* * * * * *").validate()).doesNotThrowAnyException();
        assertThatCode(() -> new CronExpression("* * * * * * [PT0S/PT0S]").validate()).doesNotThrowAnyException();
        assertThatCode(() -> new CronExpression("0 0 * * * [PT0S/PT7H]").validate()).doesNotThrowAnyException();

        assertThatCode(() -> new CronExpression("* * * * * * [PT1M/PT0S]").validate()).
                isInstanceOf(IllegalStateException.class)
                .hasMessage("The total carbon aware margin must be lower than the duration between each schedule.");
        assertThatCode(() -> new CronExpression("0 0 * * * [PT12H/PT24H]").validate()).
                isInstanceOf(IllegalStateException.class)
                .hasMessage("The total carbon aware margin must be lower than the duration between each schedule.");
    }
}