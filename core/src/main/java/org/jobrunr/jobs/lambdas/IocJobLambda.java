package org.jobrunr.jobs.lambdas;

import java.util.function.Consumer;

@FunctionalInterface
public interface IocJobLambda<T> extends Consumer<T>, JobWithIoc {
    // marker interface to make it serializable
}
