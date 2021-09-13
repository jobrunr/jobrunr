package org.jobrunr.utils;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;


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

    public static <T> boolean isNotNullOrEmpty(T[] someArray) {
        return !isNullOrEmpty(someArray);
    }

    public static <T> boolean isNullOrEmpty(T[] someArray) {
        if (someArray == null) return true;
        return someArray.length < 1;
    }

    public static <T> List<T> asList(T[] array, T... params) {
        List<T> result = new ArrayList<>();
        result.addAll(Arrays.asList(array));
        result.addAll(Arrays.asList(params));
        return result;
    }

    public static <T> ArrayList<T> asArrayList(Collection<T> existingCollection) {
        ArrayList<T> result = new ArrayList<>();
        if (existingCollection != null) {
            result.addAll(existingCollection);
        }
        return result;
    }

    public static <T> Set<T> asSet(T... items) {
        return Stream.of(items).collect(toSet());
    }

    public static <T> Set<T> asSet(Collection<T>... existingCollections) {
        Set<T> result = new HashSet<>();
        for (Collection<T> existingCollection : existingCollections) {
            result.addAll(existingCollection);
        }
        return result;
    }

    public static <K, V> Map<K, V> mapOf(K key1, V value1) {
        Map<K, V> result = new HashMap<>();
        result.put(key1, value1);
        return result;
    }

    public static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2) {
        final Map<K, V> result = mapOf(key1, value1);
        result.put(key2, value2);
        return result;
    }
}
