package org.jobrunr.scheduling;

public class ScheduleException extends RuntimeException {

    public ScheduleException(String scheduleExpression) {
        super(String.format("Schedule expression '%s' cannot be mapped to any type.", scheduleExpression));
    }
}
