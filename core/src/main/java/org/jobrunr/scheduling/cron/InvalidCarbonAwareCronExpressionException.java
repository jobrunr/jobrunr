package org.jobrunr.scheduling.cron;

public class InvalidCarbonAwareCronExpressionException extends InvalidCronExpressionException {

    public InvalidCarbonAwareCronExpressionException(String message) {
        super(message);
    }

    public InvalidCarbonAwareCronExpressionException(String message, Throwable cause) {
        super(message, cause);
    }

}
