package org.jobrunr.scheduling.carbonaware;

import org.jobrunr.scheduling.Schedule.CarbonAwareScheduleMargin;
import org.jobrunr.scheduling.cron.Cron;

import java.time.Duration;

public class CarbonAware {

    public static String using(String scheduleExpression, Duration marginBefore, Duration marginAfter) {
        return CarbonAwareScheduleMargin.margin(marginBefore, marginAfter).toScheduleExpression(scheduleExpression);
    }

    public static String using(Duration scheduleExpression, Duration marginBefore, Duration marginAfter) {
        return using(scheduleExpression.toString(), marginBefore, marginAfter);
    }

    public static String before(String scheduleExpression, Duration marginBefore) {
        return CarbonAwareScheduleMargin.before(marginBefore).toScheduleExpression(scheduleExpression);
    }

    public static String before(Duration scheduleExpression, Duration marginBefore) {
        return before(scheduleExpression.toString(), marginBefore);
    }

    public static String dailyBetween(int from, int until) {
        int marginAfter = until - from;
        return CarbonAwareScheduleMargin.after(Duration.ofHours(marginAfter)).toScheduleExpression(Cron.daily(from));
    }

    public static String dailyBefore(int until) {
        return dailyBetween(0, until);
    }
}
