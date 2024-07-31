package org.jobrunr.scheduling;

import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleExpressionTypeTest {

    @Test
    void canParseScheduleFromCronExpression() {
        Schedule schedule = ScheduleExpressionType.getSchedule("* * * * *");
        assertThat(schedule)
                .isNotNull()
                .isInstanceOf(CronExpression.class);
        assertThat(schedule.isCarbonAware()).isFalse();
    }

    @Test
    void canParseScheduleFromCronExpressionWithCarbonAwareScheduleMargin() {
        Schedule schedule = ScheduleExpressionType.getSchedule("0 0 1 * * [P1D/P3D]");
        assertThat(schedule)
                .isNotNull()
                .isInstanceOf(CronExpression.class);
        assertThat(schedule.isCarbonAware()).isTrue();
    }

    @Test
    void canParseScheduleFromInterval() {
        Schedule schedule = ScheduleExpressionType.getSchedule("P5DT6H");
        assertThat(schedule)
                .isNotNull()
                .isInstanceOf(Interval.class);
        assertThat(schedule.isCarbonAware()).isFalse();
    }

    @Test
    void canParseScheduleFromIntervalWithCarbonAwareScheduleMargin() {
        Schedule schedule = ScheduleExpressionType.getSchedule("P5DT6H [PT6H/PT0S]");
        assertThat(schedule)
                .isNotNull()
                .isInstanceOf(Interval.class);
        assertThat(schedule.isCarbonAware()).isTrue();
    }

}