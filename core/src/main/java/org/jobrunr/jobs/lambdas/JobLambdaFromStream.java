package org.jobrunr.jobs.lambdas;

import java.util.function.Consumer;

@FunctionalInterface
public interface JobLambdaFromStream<T> extends Consumer<T>, JobWithoutIoc {
    // marker interface to make it serializable
}
