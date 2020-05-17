package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface IocJobLambdaFromStream<TService, TItem> extends JobRunrJob {

    void accept(TService service, TItem item) throws Exception;
}
