package org.jobrunr.spring.annotations;

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
     * This mechanism was borrowed from {@code @Scheduled} in Spring Framework.
     */
    String CRON_DISABLED = "-";

    /**
     * @return The id of this recurring job which can be used to alter or delete it.
     */
    String id() default "";

    /**
     * The cron expression defining when to run this recurring job.
     * <p>
     * The special value {@link #CRON_DISABLED "-"} indicates a disabled cron
     * trigger, primarily meant for externally specified values resolved by a
     * <code>${...}</code> placeholder.
     *
     * @return An expression that can be parsed to a cron schedule.
     */
    String cron();

    /**
     * @return The zoneId (timezone) of when to run this recurring job.
     */
    String zoneId() default "";

}