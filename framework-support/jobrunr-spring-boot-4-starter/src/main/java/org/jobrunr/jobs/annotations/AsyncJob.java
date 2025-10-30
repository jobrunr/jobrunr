package org.jobrunr.jobs.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows scheduling a method to be executed as a background job.
 *
 * <h5>An example:</h5>
 * <pre>
 *     &#64;AsyncJob
 *     public class MyService {
 *         &#64;Job(name = "Doing some work")
 *         public void doWork() {
 *             // some long running task
 *         }
 *     }
 * </pre>
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AsyncJob {
}
