package org.jobrunr.scheduling.interval;

public class InvalidScheduleException extends RuntimeException {

    private static final String MESSAGE = "Schedule expression %s cannot be mapped to any type.";

    public InvalidScheduleException(String scheduleExpression) {
        super(String.format(MESSAGE, scheduleExpression));
    }
}
