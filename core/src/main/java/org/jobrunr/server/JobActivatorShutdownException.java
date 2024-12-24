package org.jobrunr.server;

public class JobActivatorShutdownException extends RuntimeException {

    public JobActivatorShutdownException(String message, Throwable cause) {
        super(message, cause);
    }
}
