package org.jobrunr.scheduling;

import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.jobrunr.scheduling.interval.InvalidScheduleException;

public enum ScheduleExpressionType {
    CRON_EXPRESSION,
    INTERVAL,
    INVALID,
    UNKNOWN;

    public static ScheduleExpressionType getScheduleType(String scheduleExpression) {
        if (scheduleExpression == null || scheduleExpression.isEmpty()) {
            return INVALID;
        } else if (scheduleExpression.matches(".*\\s.*")) {
            return CRON_EXPRESSION;
        } else if (scheduleExpression.startsWith("P")) {
            return INTERVAL;
        }

        return UNKNOWN;
    }

    public static Schedule createSchedule(String scheduleExpression) {
        ScheduleExpressionType type = ScheduleExpressionType.getScheduleType(scheduleExpression);

        if (type.equals(ScheduleExpressionType.CRON_EXPRESSION)) {
            return CronExpression.create(scheduleExpression);
        } else if (type.equals(ScheduleExpressionType.INTERVAL)) {
            return new Interval(scheduleExpression);
        } else {
            throw new InvalidScheduleException(scheduleExpression);
        }
    }
}
