package org.jobrunr.scheduling;

import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.cron.InvalidCronExpressionException;
import org.jobrunr.scheduling.interval.Interval;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ScheduleExpressionTypeTest {

    @Test
    void findValidSchedule() {
        assertThat(ScheduleExpressionType.findSchedule("* * * * *", "")).isEqualTo("* * * * *");
        assertThat(ScheduleExpressionType.findSchedule("", "PT10M")).isEqualTo("PT10M");
        assertThat(ScheduleExpressionType.findSchedule("-", "")).isEqualTo("-");
        assertThat(ScheduleExpressionType.findSchedule("", "-")).isEqualTo("-");

        assertThatCode(() -> ScheduleExpressionType.findSchedule("", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Either cron or interval attribute is required.");

        assertThatCode(() -> ScheduleExpressionType.findSchedule("* * * * *", "PT10M"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Both cron and interval attribute provided. Only one is allowed.");
    }

    @Test
    void createScheduleFromString() {
        assertThat(ScheduleExpressionType.createScheduleFromString("* * * * *")).isEqualTo(CronExpression.create("* * * * *"));
        assertThat(ScheduleExpressionType.createScheduleFromString("PT10M")).isEqualTo(new Interval("PT10M"));
        assertThatCode(() -> ScheduleExpressionType.createScheduleFromString("Not correct"))
                .isInstanceOf(InvalidCronExpressionException.class)
                .hasMessage("crontab expression should have 6 fields for (seconds resolution) or 5 fields for (minutes resolution)");
    }

}