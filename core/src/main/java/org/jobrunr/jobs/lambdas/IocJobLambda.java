package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface IocJobLambda<T> extends JobWithIoc {
    void accept(T var1) throws Exception;
}
