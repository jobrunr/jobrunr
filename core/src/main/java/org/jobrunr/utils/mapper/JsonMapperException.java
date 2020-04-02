package org.jobrunr.utils.mapper;

public class JsonMapperException extends RuntimeException {

    public JsonMapperException(String message) {
        super(message);
    }

    public JsonMapperException(String message, Throwable cause) {
        super(message, cause);
    }
}
