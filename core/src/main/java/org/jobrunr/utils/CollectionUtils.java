package org.jobrunr.utils;

import java.util.Collection;

public class CollectionUtils {

    private CollectionUtils() {

    }

    public static <T> boolean isNotNullOrEmpty(Collection<T> collection) {
        return !isNullOrEmpty(collection);
    }

    public static <T> boolean isNullOrEmpty(Collection<T> collection) {
        if (collection == null) return true;
        return collection.isEmpty();
    }
}
