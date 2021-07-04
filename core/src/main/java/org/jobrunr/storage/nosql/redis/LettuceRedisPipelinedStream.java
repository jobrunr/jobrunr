package org.jobrunr.storage.nosql.redis;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.jobrunr.utils.exceptions.Exceptions;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static io.lettuce.core.LettuceFutures.awaitAll;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

public class LettuceRedisPipelinedStream<T> extends AbstractPipelinedStream<T> {

    private final StatefulRedisConnection<String, String> connection;

    public LettuceRedisPipelinedStream(Collection<T> initial, StatefulRedisConnection<String, String> connection) {
        this(initial.stream(), connection);
    }

    public LettuceRedisPipelinedStream(Stream<T> initialStream, StatefulRedisConnection<String, String> connection) {
        super(initialStream);
        this.connection = connection;
    }

    public <R> LettuceRedisPipelinedStream<R> mapUsingPipeline(BiFunction<RedisAsyncCommands<String, String>, T, R> biFunction) {
        connection.setAutoFlushCommands(false);
        RedisAsyncCommands<String, String> redisAsyncCommands = connection.async();
        List<R> collect = initialStream
                .map(item -> biFunction.apply(redisAsyncCommands, item))
                .collect(toList()); // must collect otherwise map is not executed
        connection.flushCommands();
        awaitAll(ofSeconds(10), collect.toArray(new RedisFuture[0]));
        return new LettuceRedisPipelinedStream<>(collect, connection);
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
    public LettuceRedisPipelinedStream<T> limit(long l) {
        return new LettuceRedisPipelinedStream<>(initialStream.limit(l), connection);
    }

    @Override
    public LettuceRedisPipelinedStream<T> skip(long l) {
        return new LettuceRedisPipelinedStream<>(initialStream.skip(l), connection);
    }
}
