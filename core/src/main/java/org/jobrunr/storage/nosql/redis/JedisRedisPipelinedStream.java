package org.jobrunr.storage.nosql.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class JedisRedisPipelinedStream<T> extends AbstractPipelinedStream<T> {

    private final Jedis jedis;

    public JedisRedisPipelinedStream(Collection<T> initial, Jedis jedis) {
        this(initial.stream(), jedis);
    }

    public JedisRedisPipelinedStream(Stream<T> initialStream, Jedis jedis) {
        super(initialStream);
        this.jedis = jedis;
    }

    public <R> JedisRedisPipelinedStream<R> mapUsingPipeline(BiFunction<Pipeline, T, R> biFunction) {
        List<R> collect;
        try (final Pipeline pipeline = jedis.pipelined()) {
            collect = initialStream
                    .map(item -> biFunction.apply(pipeline, item))
                    .collect(toList()); // must collect otherwise map is not executed
            pipeline.sync();
        }
        return new JedisRedisPipelinedStream<>(collect, jedis);
    }

    public <R> JedisRedisPipelinedStream<R> mapAfterSync(Function<? super T, ? extends R> function) {
        return new JedisRedisPipelinedStream<>(initialStream.map(function), jedis);
    }

    @Override
    public JedisRedisPipelinedStream<T> limit(long l) {
        return new JedisRedisPipelinedStream<>(initialStream.limit(l), jedis);
    }

    @Override
    public JedisRedisPipelinedStream<T> skip(long l) {
        return new JedisRedisPipelinedStream<>(initialStream.skip(l), jedis);
    }
}
