package org.jobrunr.scheduling;

import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.jobrunr.utils.StringUtils;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

public enum ScheduleExpressionType {
    CRON_EXPRESSION {
        @Override
        public Schedule createSchedule(String scheduleExpression) {
            return new CronExpression(scheduleExpression);
        }
    },
    INTERVAL {
        @Override
        public Schedule createSchedule(String scheduleExpression) {
            return new Interval(scheduleExpression);
        }
    };

    public static Schedule createScheduleFromString(String scheduleExpression) {
        if (isNotNullOrEmpty(scheduleExpression)) {
            if (scheduleExpression.matches(".*\\s.*")) {
                return CRON_EXPRESSION.createSchedule(scheduleExpression);
            } else if (scheduleExpression.startsWith("P")) {
                return INTERVAL.createSchedule(scheduleExpression);
            }
        }
        throw new ScheduleException(scheduleExpression);
    }

    public static String selectConfiguredScheduleExpression(String cron, String interval) {
        List<String> validScheduleExpressions = Stream.of(cron, interval).filter(StringUtils::isNotNullOrEmpty).collect(toList());
        int count = validScheduleExpressions.size();
        if (count == 0) throw new IllegalArgumentException("Either cron or interval attribute is required.");
        if (count > 1) throw new IllegalArgumentException("Both cron and interval attribute provided. Only one is allowed.");
        return validScheduleExpressions.get(0);
    }

    public abstract Schedule createSchedule(String scheduleExpression);
}
