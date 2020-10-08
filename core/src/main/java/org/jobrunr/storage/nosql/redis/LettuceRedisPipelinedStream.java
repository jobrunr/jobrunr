package org.jobrunr.storage.nosql.redis;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.jobrunr.utils.exceptions.Exceptions;

import java.time.Duration;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.util.stream.Collectors.toList;

public class LettuceRedisPipelinedStream<T> implements Stream<T> {

    private final Stream<T> initialStream;
    private final StatefulRedisConnection connection;

    public LettuceRedisPipelinedStream(Collection<T> initial, StatefulRedisConnection connection) {
        this(initial.stream(), connection);
    }

    public LettuceRedisPipelinedStream(Stream<T> initialStream, StatefulRedisConnection connection) {
        this.initialStream = initialStream;
        this.connection = connection;
    }

    public <R> LettuceRedisPipelinedStream<R> mapUsingPipeline(BiFunction<RedisAsyncCommands, T, R> biFunction) {
        connection.setAutoFlushCommands(false);
        RedisAsyncCommands redisAsyncCommands = connection.async();
        List<R> collect = initialStream
                .map(item -> biFunction.apply(redisAsyncCommands, item))
                .collect(toList()); // must collect otherwise map is not executed
        connection.flushCommands();
        LettuceFutures.awaitAll(Duration.ofSeconds(10), collect.toArray(new RedisFuture[collect.size()]));
        return new LettuceRedisPipelinedStream<>(collect, connection);
    }

    public LettuceRedisPipelinedStream<Map<String, String>> mapToValues() {
        return mapAfterSync(redisFuture -> (Map<String, String>) ((RedisFuture) redisFuture).get());
    }

    public <R> LettuceRedisPipelinedStream<R> mapAfterSync(Exceptions.ThrowingFunction<? super T, ? extends R> function) {
        Stream<R> rStream = initialStream.map(x -> {
            try {
                return function.apply(x);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        return new LettuceRedisPipelinedStream<>(rStream, connection);
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        return initialStream.filter(predicate);
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> function) {
        return initialStream.map(function);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> toIntFunction) {
        return initialStream.mapToInt(toIntFunction);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> toLongFunction) {
        return initialStream.mapToLong(toLongFunction);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> toDoubleFunction) {
        return initialStream.mapToDouble(toDoubleFunction);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> function) {
        return initialStream.flatMap(function);
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> function) {
        return initialStream.flatMapToInt(function);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> function) {
        return initialStream.flatMapToLong(function);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> function) {
        return initialStream.flatMapToDouble(function);
    }

    @Override
    public Stream<T> distinct() {
        return initialStream.distinct();
    }

    @Override
    public Stream<T> sorted() {
        return initialStream.sorted();
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        return initialStream.sorted(comparator);
    }

    @Override
    public Stream<T> peek(Consumer<? super T> consumer) {
        return initialStream.peek(consumer);
    }

    @Override
    public LettuceRedisPipelinedStream<T> limit(long l) {
        return new LettuceRedisPipelinedStream<>(initialStream.limit(l), connection);
    }

    @Override
    public LettuceRedisPipelinedStream<T> skip(long l) {
        return new LettuceRedisPipelinedStream<>(initialStream.skip(l), connection);
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        initialStream.forEach(consumer);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> consumer) {
        initialStream.forEachOrdered(consumer);
    }

    @Override
    public Object[] toArray() {
        return initialStream.toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> intFunction) {
        return initialStream.toArray(intFunction);
    }

    @Override
    public T reduce(T t, BinaryOperator<T> binaryOperator) {
        return initialStream.reduce(t, binaryOperator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> binaryOperator) {
        return initialStream.reduce(binaryOperator);
    }

    @Override
    public <U> U reduce(U u, BiFunction<U, ? super T, U> biFunction, BinaryOperator<U> binaryOperator) {
        return initialStream.reduce(u, biFunction, binaryOperator);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> biConsumer, BiConsumer<R, R> biConsumer1) {
        return initialStream.collect(supplier, biConsumer, biConsumer1);
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return initialStream.collect(collector);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return initialStream.min(comparator);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return initialStream.max(comparator);
    }

    @Override
    public long count() {
        return initialStream.count();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return initialStream.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return initialStream.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return initialStream.noneMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        return initialStream.findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return initialStream.findAny();
    }

    public static <T1> Builder<T1> builder() {
        return Stream.builder();
    }

    public static <T1> Stream<T1> empty() {
        return Stream.empty();
    }

    public static <T1> Stream<T1> of(T1 t1) {
        return Stream.of(t1);
    }

    @SafeVarargs
    public static <T1> Stream<T1> of(T1... values) {
        return Stream.of(values);
    }

    public static <T1> Stream<T1> iterate(T1 seed, UnaryOperator<T1> f) {
        return Stream.iterate(seed, f);
    }

    public static <T1> Stream<T1> generate(Supplier<? extends T1> s) {
        return Stream.generate(s);
    }

    public static <T1> Stream<T1> concat(Stream<? extends T1> a, Stream<? extends T1> b) {
        return Stream.concat(a, b);
    }

    @Override
    public Iterator<T> iterator() {
        return initialStream.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return initialStream.spliterator();
    }

    @Override
    public boolean isParallel() {
        return initialStream.isParallel();
    }

    @Override
    public Stream<T> sequential() {
        return initialStream.sequential();
    }

    @Override
    public Stream<T> parallel() {
        return initialStream.parallel();
    }

    @Override
    public Stream<T> unordered() {
        return initialStream.unordered();
    }

    @Override
    public Stream<T> onClose(Runnable runnable) {
        return initialStream.onClose(runnable);
    }

    @Override
    public void close() {
        initialStream.close();
    }
}
