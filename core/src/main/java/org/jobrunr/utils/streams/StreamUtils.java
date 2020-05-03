package org.jobrunr.utils.streams;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StreamUtils {

    private StreamUtils() {
    }

    /**
     * Creates a new batch collector
     *
     * @param batchSize      the batch size after which the batchProcessor should be called
     * @param batchProcessor the batch processor which accepts batches of records to process
     * @param <T>            the type of elements being processed
     * @return a batch collector instance
     */
    public static <T> Collector<T, List<T>, List<T>> batchCollector(int batchSize, Consumer<List<T>> batchProcessor) {
        return new BatchCollector<>(batchSize, batchProcessor);
    }

    public static <X, Y> Stream<Y> ofType(List<X> items, Class<Y> clazz) {
        return ofType(items.stream(), clazz);
    }

    public static <X, Y> Stream<Y> ofType(Stream<X> stream, Class<Y> clazz) {
        return stream
                .filter(clazz::isInstance)
                .map(clazz::cast);
    }

    public static <T> Collector<T, ?, T> single() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException();
                    }
                    return list.get(0);
                }
        );
    }

}