package org.jobrunr.storage;

import org.jspecify.annotations.Nullable;

public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, @Nullable Throwable throwable) {
        super(message, throwable);
    }

    public StorageException(Throwable cause) {
        super(cause);
    }
}
