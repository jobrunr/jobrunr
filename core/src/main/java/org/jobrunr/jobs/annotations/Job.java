package org.jobrunr.jobs.annotations;

import org.jobrunr.jobs.filters.JobFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows to add a specific name to a job that will be used in the dashboard as well as extra jobFilters that will be used for the job.
 * The annotation can be used on the method that is referenced in the lambda.
 *
 * <h5>An example:</h5>
 * <pre>
 *      public class MyService {
 *
 *          &commat;Job(name = "Doing some work", jobFilters = {TheSunIsAlwaysShiningElectStateFilter.class, TestFilter.class})
 *          public void doWork() {
 *              // some long running task
 *          }
 *      }
 *
 *      MyService service = new MyService();
 *      BackgroundJob.enqueue(() -> service.doWork());
 * </pre>
 * <p>
 * In the Job name you can also reference parameters which where passed to the method.
 *
 * <h5>An example:</h5>
 * <pre>
 *       public class MyService {
 *
 *           &commat;Job(name = "Doing some work for user %s", jobFilters = {TheSunIsAlwaysShiningElectStateFilter.class, TestFilter.class})
 *           public void doWork(String userName) {
 *               // some long running task
 *           }
 *       }
 *
 *       MyService service = new MyService();
 *       BackgroundJob.enqueue(() -> service.doWork("Ronald"));
 *  </pre>
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Job {

    String name() default "";

    Class<? extends JobFilter>[] jobFilters() default {};

}
