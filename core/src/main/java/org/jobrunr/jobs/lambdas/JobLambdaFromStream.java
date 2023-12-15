package org.jobrunr.jobs.lambdas;

/**
 * This is a functional interface which allows you to schedule jobs based on a Stream and a lambda that will be parsed by JobRunr.
 * You <strong>may not create an actual instance of this class</strong>, instead you use it as follows:
 *
 * <pre>{@code
 *
 *     @Inject
 *     MyService myService;
 *
 *     Stream<User> userStream = userRepository.getAllUsers();
 *     BackgroundJob.enqueue(userStream, (user) -> myService.doWork("do some work for user " + user.getId()));
 * }</pre>
 * <p>
 * or
 * <pre>{@code
 *
 *      @Inject
 *      MyService myService;
 *
 *      Stream<User> userStream = userRepository.getAllUsers();
 *      jobScheduler.enqueue(userStream, (user) -> myService.doWork("do some work for user " + user.getId()));
 * }</pre>
 * <p>
 * This functional interface allows you to enqueue background jobs for each item in the stream while having an actual instance available
 * of your service.
 * While processing, JobRunr will lookup the actual service in the IoC container or create a new instance using the default constructor.
 *
 * @param <T> The item returned by the Stream
 */
@FunctionalInterface
public interface JobLambdaFromStream<T> extends JobRunrJob {

    void accept(T item) throws Exception;
}
