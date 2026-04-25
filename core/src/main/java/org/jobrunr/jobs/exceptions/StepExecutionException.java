package org.jobrunr.jobs.exceptions;

import org.jobrunr.JobRunrException;

/**
 * An exception that is thrown if a certain step inside a Job fails
 */
public class StepExecutionException extends JobRunrException {

    public StepExecutionException(String message, Exception exception) {
        super(message, isJobRunrExceptionThatMustNotBeRetried(exception), exception);
    }

    private static boolean isJobRunrExceptionThatMustNotBeRetried(Exception exception) {
        return exception instanceof JobRunrException && ((JobRunrException) exception).isProblematicAndDoNotRetry();
    }
}
