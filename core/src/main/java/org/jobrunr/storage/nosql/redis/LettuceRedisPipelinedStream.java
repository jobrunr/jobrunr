package org.jobrunr.storage.nosql.redis;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.jobrunr.utils.exceptions.Exceptions;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class LettuceRedisPipelinedStream<T> extends AbstractPipelinedStream<T> {

    private final StatefulRedisConnection connection;

    public LettuceRedisPipelinedStream(Collection<T> initial, StatefulRedisConnection connection) {
        this(initial.stream(), connection);
    }

    public LettuceRedisPipelinedStream(Stream<T> initialStream, StatefulRedisConnection connection) {
        super(initialStream);
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
    public LettuceRedisPipelinedStream<T> limit(long l) {
        return new LettuceRedisPipelinedStream<>(initialStream.limit(l), connection);
    }

    @Override
    public LettuceRedisPipelinedStream<T> skip(long l) {
        return new LettuceRedisPipelinedStream<>(initialStream.skip(l), connection);
    }
}
