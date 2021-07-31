package org.jobrunr.annotations;

import java.lang.annotation.*;

/**
 * Allows to recurrently schedule a method from a Spring Service bean using JobRunr.
 *
 * <em>Note that methods annotated with the &commat;Recurring annotation may not have any parameters.</em>
 *
 * <h5>An example:</h5>
 * <pre>
 *      public class MyService {
 *
 *          &commat;Recurring(id = "my-recurring-job", cron = "**&#47;5 * * * *")
 *          &commat;Job(name = "Doing some work")
 *          public void doWork() {
 *              // some long running task
 *          }
 *      }
 * </pre>
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Recurring {

    String id() default "";

    String cron();

    String zoneId() default "";

}