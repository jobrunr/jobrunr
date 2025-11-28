package org.jobrunr.utils.streams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.Objects.requireNonNull;

/**
 * Collects elements in the stream and calls the supplied batch processor
 * after the configured batch size is reached.
 * <p>
 * Parallel streams are not supported and an {@link UnsupportedOperationException} will be thrown if the Stream is processed in parallel.
 * <p>
 * The elements are not kept in memory, and the final result is the total number of batched items.
 *
 * @param <T> Type of the elements being collected
 */
class BatchCollector<T> implements Collector<T, List<T>, Long> {

    private final int batchSize;
    private final Consumer<List<T>> batchProcessor;
    private long totalAmountBatched;

    /**
     * Constructs the batch collector
     *
     * @param batchSize      the batch size after which the batchProcessor should be called
     * @param batchProcessor the batch processor which accepts batches of records to process
     */
    BatchCollector(int batchSize, Consumer<List<T>> batchProcessor) {
        this.batchSize = batchSize;
        this.batchProcessor = requireNonNull(batchProcessor);
    }

    @Override
    public Supplier<List<T>> supplier() {
        this.totalAmountBatched = 0;
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<T>, T> accumulator() {
        return (ts, t) -> {
            ts.add(t);
            if (ts.size() >= batchSize) {
                batchProcessor.accept(ts);
                ts.clear();
                totalAmountBatched += batchSize;
            }
        };
    }

    @Override
    public BinaryOperator<List<T>> combiner() {
        return (ts, ots) -> {
            throw new UnsupportedOperationException("Parallel streams are not supported.");
        };
    }

    @Override
    public Function<List<T>, Long> finisher() {
        return ts -> {
            batchProcessor.accept(ts);
            return totalAmountBatched + ts.size();
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}