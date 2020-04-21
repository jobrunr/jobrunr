package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface JobLambdaFromStream<T> extends JobWithoutIoc {
    // marker interface to make it serializable
    void accept(T var1) throws Exception;
}
