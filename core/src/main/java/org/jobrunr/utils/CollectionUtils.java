package org.jobrunr.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
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

    public static <T> Optional<T> findFirst(List<T> items) {
        return ofNullable(getFirst(items));
    }

    public static <T> Optional<T> findLast(List<T> items) {
        return ofNullable(getLast(items));
    }

    public static <T> T getFirst(List<T> items) {
        if (isNullOrEmpty(items)) return null;
        return items.get(0);
    }

    public static <T> T getLast(List<T> items) {
        if (isNullOrEmpty(items)) return null;
        return items.get(items.size() - 1);
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

    public static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2, K key3, V value3) {
        final Map<K, V> result = mapOf(key1, value1, key2, value2);
        result.put(key3, value3);
        return result;
    }

    public static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
        final Map<K, V> result = mapOf(key1, value1, key2, value2, key3, value3);
        result.put(key4, value4);
        return result;
    }

    public static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5) {
        final Map<K, V> result = mapOf(key1, value1, key2, value2, key3, value3, key4, value4);
        result.put(key5, value5);
        return result;
    }

    public static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6) {
        final Map<K, V> result = mapOf(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5);
        result.put(key6, value6);
        return result;
    }

    public static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7) {
        final Map<K, V> result = mapOf(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5, key6, value6);
        result.put(key7, value7);
        return result;
    }

    public static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7, K key8, V value8) {
        final Map<K, V> result = mapOf(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5, key6, value6, key7, value7);
        result.put(key8, value8);
        return result;
    }
}
