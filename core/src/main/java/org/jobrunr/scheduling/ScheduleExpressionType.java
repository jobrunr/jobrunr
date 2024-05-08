package org.jobrunr.scheduling;

import org.jobrunr.scheduling.cron.CarbonAwareCronExpression;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;

import java.time.Duration;
import java.util.Objects;

public enum ScheduleExpressionType {
    CRON_EXPRESSION {
        @Override
        public Schedule createSchedule(String scheduleExpression) {
            return CronExpression.create(scheduleExpression);
        }
    },
    CARBON_AWARE_CRON_EXPRESSION {
        @Override
        public Schedule createSchedule(String scheduleExpression) {
            return CarbonAwareCronExpression.create(scheduleExpression);
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
            if (scheduleExpression.startsWith("P")) {
                return INTERVAL.createSchedule(scheduleExpression);
            } else if (isCarbonAwareCronExpression(scheduleExpression)) {
                return CARBON_AWARE_CRON_EXPRESSION.createSchedule(scheduleExpression);
            } else if (scheduleExpression.matches(".*\\s.*")) {
                return CRON_EXPRESSION.createSchedule(scheduleExpression);
            }
        }
        throw new ScheduleException(scheduleExpression);
    }

    public abstract Schedule createSchedule(String scheduleExpression);

    private static boolean isCarbonAwareCronExpression(String expression) {
        String[] parts = expression.split("\\s+");
        try {
            Duration.parse(parts[parts.length - 1]);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
