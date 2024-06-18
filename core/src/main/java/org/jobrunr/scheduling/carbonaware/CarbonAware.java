package org.jobrunr.scheduling.carbonaware;

import java.time.Duration;

public class CarbonAware {

    public static String using(String scheduleExpression, Duration marginBefore) {
        return "";
    }

    public static String using(String scheduleExpression, Duration marginBefore, Duration marginAfter) {
        return "";
    }

    public static String using(Duration scheduleExpression, Duration marginBefore) {
        return "";
    }

    public static String using(Duration scheduleExpression, Duration marginBefore, Duration marginAfter) {
        return "";
    }

    public static String before(String scheduleExpression, Duration margin) {
        return scheduleExpression + " [" + margin + "]";
    }

    public static String dailyBetween(int from, int until) {
        return null;
    }

    public static String dailyBefore(int until) {
        return null;
    }
}
