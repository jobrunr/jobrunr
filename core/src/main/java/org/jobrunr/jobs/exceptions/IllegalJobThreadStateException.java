package org.jobrunr.jobs.exceptions;

public class IllegalJobThreadStateException extends IllegalThreadStateException {

    public IllegalJobThreadStateException(String s) {
        super(s);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
