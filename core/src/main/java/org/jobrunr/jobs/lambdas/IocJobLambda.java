package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface IocJobLambda<T> extends JobRunrJob {

    void accept(T service) throws Exception;
}
