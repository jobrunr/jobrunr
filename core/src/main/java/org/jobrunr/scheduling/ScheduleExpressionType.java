package org.jobrunr.scheduling;

import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;

import java.util.Objects;

public enum ScheduleExpressionType {
    CRON_EXPRESSION {
        @Override
        public Schedule createSchedule(String scheduleExpression) {
            return CronExpression.create(scheduleExpression);
        }
    },
    INTERVAL {
        @Override
        public Schedule createSchedule(String scheduleExpression) {
            return new Interval(scheduleExpression);
        }
    };

    public static Schedule getSchedule(String scheduleExpression) {
        if (Objects.nonNull(scheduleExpression) && !scheduleExpression.isEmpty()) {
            if (scheduleExpression.matches(".*\\s.*")) {
                return CRON_EXPRESSION.createSchedule(scheduleExpression);
            } else if (scheduleExpression.startsWith("P")) {
                return INTERVAL.createSchedule(scheduleExpression);
            }
        }
        throw new ScheduleException(scheduleExpression);
    }

    public abstract Schedule createSchedule(String scheduleExpression);
}
