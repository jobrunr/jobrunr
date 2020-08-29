package org.jobrunr.jobs.lambdas;

/**
 * This is a functional interface which represents a lambda that will be parsed by JobRunr.
 * You <strong>may not create an actual instance of this class</strong>, instead you use it as follows:
 *
 * <pre>{@code
 *     @Inject
 *     MyService myService;
 *
 *     BackgroundJob.enqueue(myService -> myService.doWork("some argument"))
 * }</pre>
 * <p>
 * or
 * <pre>{@code
 *     @Inject
 *     MyService myService;
 *
 *     jobScheduler.enqueue(myService -> myService.doWork("some argument"))
 * }</pre>
 * <p>
 * This functional interface allows you to enqueue background jobs while having an actual instance available of your service.
 * While processing, JobRunr will lookup the actual service in the IoC container or create a new instance using the default constructor.
 */
@FunctionalInterface
public interface JobLambda extends JobRunrJob {

    void run() throws Exception;
}
