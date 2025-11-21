package org.jobrunr.utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ObjectUtils {

    public static @NonNull <T> T ensureNonNull(@Nullable T obj) {
        if (obj == null) {
            throw new IllegalStateException("Object must not be null in this context.");
        }
        return obj;
    }
}
