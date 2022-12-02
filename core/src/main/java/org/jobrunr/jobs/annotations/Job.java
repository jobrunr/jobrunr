package org.jobrunr.jobs.annotations;

import org.jobrunr.jobs.filters.JobFilter;

import java.lang.annotation.*;

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
 *      BackgroundJob.enqueue(() -&gt; service.doWork());
 * </pre>
 * <p>
 * In the Job name you can also reference parameters which where passed to the method. This is done by means of the syntax <em>%{index}</em> where index is the zero-based index of your parameters.
 *
 * <h5>An example:</h5>
 * <pre>
 *       public class MyService {
 *
 *           &commat;Job(name = "Doing some work for user %0", jobFilters = {TheSunIsAlwaysShiningElectStateFilter.class, TestFilter.class})
 *           public void doWork(String userName) {
 *               // some long running task
 *           }
 *       }
 *
 *       MyService service = new MyService();
 *       BackgroundJob.enqueue(() -&gt; service.doWork("Ronald"));
 *  </pre>
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Job {

    int NBR_OF_RETRIES_NOT_PROVIDED = -1;

    /**
     * The name of the job. Parameter substitution is supported by means of <code>%0</code> (this will be replaced by the toString representation of the first argument).
     * @return the name of the job.
     */
    String name() default "";

    int retries() default NBR_OF_RETRIES_NOT_PROVIDED;

    /**
     * The labels for the job. Parameter substitution is supported by means of <code>%0</code> (this will be replaced by the toString representation of the first argument).
     * @return the labels for the job.
     */
    String[] labels() default {};

    Class<? extends JobFilter>[] jobFilters() default {};
}
