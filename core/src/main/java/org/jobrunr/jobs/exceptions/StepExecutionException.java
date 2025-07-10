package org.jobrunr.jobs.exceptions;

/**
 * An exception that is thrown if a certain step inside a Job fails
 */
public class StepExecutionException extends RuntimeException {

    public StepExecutionException(String message, Exception exception) {
        super(message, exception);
    }
}
