package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface IocJobLambda<T> extends JobWithIoc {
    // marker interface to make it serializable
    void accept(T var1) throws Exception;
}
