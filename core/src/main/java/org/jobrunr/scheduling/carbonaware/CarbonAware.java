package org.jobrunr.scheduling.carbonaware;

import org.jobrunr.scheduling.Schedule.CarbonAwareScheduleMargin;

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
        return null;
    }

    public static String dailyBefore(int until) {
        return null;
    }
}
