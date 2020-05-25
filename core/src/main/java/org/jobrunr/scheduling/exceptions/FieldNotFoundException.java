package org.jobrunr.scheduling.exceptions;

import org.jobrunr.JobRunrException;

public class FieldNotFoundException extends JobRunrException {

    public FieldNotFoundException(Class<?> clazz, String fieldName) {
        super(clazz + "." + fieldName);
    }
}
