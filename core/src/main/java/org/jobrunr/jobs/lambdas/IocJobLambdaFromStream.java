package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface IocJobLambdaFromStream<TService, TItem> extends JobWithIoc {
    // marker interface to make it serializable
    void accept(TService var1, TItem var2) throws Exception;
}
