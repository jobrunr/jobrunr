package org.jobrunr.scheduling.cron;

public class InvalidCronExpressionException extends RuntimeException {

    InvalidCronExpressionException(String message) {
        super(message);
    }

    InvalidCronExpressionException(String message, Throwable cause) {
        super(message, cause);
    }

}
