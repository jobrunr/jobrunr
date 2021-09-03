package org.jobrunr.utils.mapper;

public class JobParameterJsonMapperException extends IllegalArgumentException {

    public JobParameterJsonMapperException(String message) {
        super(message);
    }

    public JobParameterJsonMapperException(String message, Throwable cause) {
        super(message, cause);
    }
}
