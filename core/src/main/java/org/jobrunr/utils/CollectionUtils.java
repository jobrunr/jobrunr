package org.jobrunr.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

    public static <T> List<T> asList(T[] array, T... params) {
        List<T> result = new ArrayList<>();
        result.addAll(Arrays.asList(array));
        result.addAll(Arrays.asList(params));
        return result;
    }
}
