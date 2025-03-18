package org.jobrunr.utils;

import java.util.Optional;

public class OptionalUtils {

    public static boolean isPresent(Optional<?> optional) {
        return optional.isPresent();
    }

    public static boolean isNotPresent(Optional<?> optional) {
        return !isPresent(optional);
    }
}
