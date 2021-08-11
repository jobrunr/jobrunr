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

    /**
     * A special cron expression value that indicates a disabled trigger: {@value}.
     * <p>
     * This is primarily meant for use with <code>${...}</code> placeholders,
     * allowing for external disabling of corresponding recurring methods.
     * <p>
     * This mechanism was borrowed from {@code @Scheduled} from Spring Framework.
     */
    String CRON_DISABLED = "-";

    String id() default "";

    String cron();

    String zoneId() default "";

}