package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface JobLambdaFromStream<T> extends JobRunrJob {

    void accept(T item) throws Exception;
}
