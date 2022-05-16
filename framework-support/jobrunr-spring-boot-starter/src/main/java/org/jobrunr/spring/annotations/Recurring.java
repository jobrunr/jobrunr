package org.jobrunr.spring.annotations;

import java.lang.annotation.*;

/**
 * Allows to recurrently schedule a method from a Spring Service bean using JobRunr.
 *
 * <em>Note that methods annotated with the &commat;Recurring annotation may only have zero parameters or a single parameter of type org.jobrunr.jobs.context.JobContext.</em>
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
     * A special cron/interval expression value that indicates a disabled trigger: {@value}.
     * <p>
     * This is primarily meant for use with <code>${...}</code> placeholders,
     * allowing for external disabling of corresponding recurring methods.
     * <p>
     * This mechanism was borrowed from {@code @Scheduled} in Spring Framework.
     */
    String RECURRING_JOB_DISABLED = "-";

    /**
     * @return The id of this recurring job which can be used to alter or delete it.
     */
    String id() default "";

    /**
     * The cron expression defining when to run this recurring job.
     * <p>
     * The special value {@link #RECURRING_JOB_DISABLED "-"} indicates a disabled cron
     * trigger, primarily meant for externally specified values resolved by a
     * <code>${...}</code> placeholder.
     *
     * @return An expression that can be parsed to a cron schedule.
     */
    String cron() default "";

    /**
     * The time interval between scheduled runs pf this recurring job.
     * <p>@see Duration</p>
     * <p>Examples:</p>
     * <pre>
     * "PT20S"     -- 20 seconds
     * "PT15M"     -- 15 minutes
     * "PT10H"     -- 10 hours
     * "P2D"       -- 2 days
     * "P2DT3H4M"  -- 2 days, 3 hours and 4 minutes
     * </pre>
     */
    String interval() default "";

    /**
     * @return The zoneId (timezone) of when to run this recurring job.
     */
    String zoneId() default "";

}
