package org.jobrunr.jobs.lambdas;

/**
 * This is a functional interface which represents a lambda that will be parsed by JobRunr.
 * You <strong>may not create an actual instance of this class</strong>, instead you use it as follows:
 *
 * <pre>{@code
 *     BackgroundJob.<SomeService>enqueue(x -> x.doWork("some argument"))
 * }</pre>
 * <p>
 * or
 * <pre>{@code
 *     jobScheduler.<SomeService>enqueue(x -> x.doWork("some argument"))
 * }</pre>
 * <p>
 * This functional interface allows you to enqueue background jobs without having an actual instance available of your service.
 * While processing, JobRunr will lookup the actual service in the IoC container or create a new instance using the default constructor.
 *
 * @param <S> Your service on which you want to call a background job method.
 */

@FunctionalInterface
public interface IocJobLambda<S> extends JobRunrJob {

    void accept(S service) throws Exception;
}
