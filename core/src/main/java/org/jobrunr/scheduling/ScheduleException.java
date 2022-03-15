package org.jobrunr.scheduling;

public class ScheduleException extends RuntimeException {

    private static final String MESSAGE = "Schedule expression '%s' cannot be mapped to any type.";

    public ScheduleException(String scheduleExpression) {
        super(String.format(MESSAGE, scheduleExpression));
    }
}
