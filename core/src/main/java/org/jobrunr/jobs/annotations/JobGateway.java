package org.jobrunr.jobs.annotations;

import java.lang.annotation.*;

/**
* Allows scheduling a method to be executed as a background job.
 *
 * <h5>An example:</h5>
 * <pre>
 *     &#64;JobGateway
 *     public class MyService {
 *         &#64;Job(name = "Doing some work")
 *         public void doWork() {
 *             // some long running task
 *         }
 *     }
 * </pre>
 *
*/

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface JobGateway {
}
