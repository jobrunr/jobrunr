package org.jobrunr.jobs.lambdas;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface IocJobLambdaFromStream<TService, TItem> extends BiConsumer<TService, TItem>, JobWithIoc {
    // marker interface to make it serializable
}
