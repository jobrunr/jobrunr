package org.jobrunr.scheduling.carbonaware;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.CarbonAwareScheduleMargin;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;

import java.time.Duration;
import java.time.temporal.Temporal;

import static java.lang.String.format;
import static java.time.Instant.now;
import static org.jobrunr.utils.InstantUtils.toInstant;


/**
 * Utility class for everything related to Carbon-Aware Scheduling.
 * <p>
 * For scheduling jobs, you can use:
 * <ul>
 *     <li>TODO</li>
 * </ul>
 * <p>
 * For recurring jobs, you can use:
 * <ul>
 *     <li>{@link CarbonAware#cron(String, Duration)}</li>
 *     <li>{@link CarbonAware#cron(String, Duration, Duration)}</li>
 *     <li>{@link CarbonAware#interval(Duration, Duration, Duration)}</li>
 * </ul>
 */
public class CarbonAware {

    /**
     * Allows to relax schedule of a job to minimize carbon impact.
     * The job will run before the provided {@code to} Temporal instance.
     *
     * @param to the time expressed in java.time.Temporal before which the job must be scheduled.
     * @return A carbon aware period between {@code Instant.now()} and the provided {@code to}.
     */
    public static CarbonAwarePeriod before(Temporal to) {
        return between(now(), to);
    }

    /**
     * Allows to relax schedule of a job to minimize carbon impact.
     * The job will run between the two provided {@code from} and {@code to} Temporal instances as the interval.
     *
     * @param from the start time expressed in java.time.Temporal of the carbon aware margin.
     * @param to   the end time expressed in java.time.Temporal of the carbon aware margin.
     * @return A carbon aware period between the provided {@code from} and {@code to}.
     */
    public static CarbonAwarePeriod between(Temporal from, Temporal to) {
        return new CarbonAwarePeriod(toInstant(from), toInstant(to));
    }

    /**
     * Allows to relax the schedule of a {@link RecurringJob} to minimize carbon impact.
     * The RecurringJob may run {@code marginBefore} earlier and {@code marginAfter} later than originally planned.
     *
     * @param scheduleExpression   the scheduleExpression may be a {@link CronExpression} or a Duration (for {@link Interval} scheduling).
     * @param marginBeforeAndAfter the amount of time the {@link RecurringJob} is allowed to run before and after its original schedule.
     * @return A carbon aware schedule expression.
     */
    public static String cron(String scheduleExpression, Duration marginBeforeAndAfter) {
        return cron(scheduleExpression, marginBeforeAndAfter, marginBeforeAndAfter);
    }

    /**
     * Allows to relax the schedule of a {@link RecurringJob} to minimize carbon impact.
     * The RecurringJob may run {@code marginBefore} earlier and {@code marginAfter} later than originally planned.
     *
     * @param scheduleExpression the scheduleExpression may be a {@link CronExpression} or a Duration (for {@link Interval} scheduling).
     * @param marginBefore       the amount of time the {@link RecurringJob} is allowed to run before its original schedule.
     * @param marginAfter        the amount of time the {@link RecurringJob} is allowed to run after its original schedule.
     * @return A carbon aware schedule expression.
     */
    public static String cron(String scheduleExpression, Duration marginBefore, Duration marginAfter) {
        return CarbonAwareScheduleMargin.margin(marginBefore, marginAfter).toScheduleExpression(scheduleExpression);
    }

    /**
     * Allows to relax the {@link Interval} schedule of a {@link RecurringJob} to minimize carbon impact.
     * The RecurringJob may run {@code marginBefore} earlier and {@code marginAfter} later than original planned.
     *
     * @param duration             the scheduleExpression, a Duration for {@link Interval} scheduling.
     * @param marginBeforeAndAfter the amount of time the {@link RecurringJob} is allowed to run before and after its original schedule.
     * @return A carbon aware {@link Interval} schedule expression.
     */
    public static String interval(Duration duration, Duration marginBeforeAndAfter) {
        return interval(duration, marginBeforeAndAfter, marginBeforeAndAfter);
    }

    /**
     * Allows to relax the {@link Interval} schedule of a {@link RecurringJob} to minimize carbon impact.
     * The RecurringJob may run {@code marginBefore} earlier and {@code marginAfter} later than original planned.
     *
     * @param duration     the scheduleExpression, a Duration for {@link Interval} scheduling.
     * @param marginBefore the amount of time the {@link RecurringJob} is allowed to run before its original schedule.
     * @param marginAfter  the amount of time the {@link RecurringJob} is allowed to run after its original schedule.
     * @return A carbon aware {@link Interval} schedule expression.
     */
    public static String interval(Duration duration, Duration marginBefore, Duration marginAfter) {
        return CarbonAwareScheduleMargin.margin(marginBefore, marginAfter).toScheduleExpression(duration.toString());
    }

    /**
     * Allows to relax the daily schedule of a {@link RecurringJob} to minimize carbon impact.
     * The {@link RecurringJob} may run daily between the hours of {@code from} and {@code until} (in 24-hour format).
     *
     * @param fromHour  the hour at which the {@link RecurringJob} is ready to be scheduled (in 24-hour format).
     * @param untilHour the hour before which the {@link RecurringJob} must be scheduled (in 24-hour format).
     * @return A carbon aware schedule expression.
     */
    public static String dailyBetween(int fromHour, int untilHour) {
        if (fromHour < 0 || untilHour < 0 || fromHour > 23 || untilHour > 23) {
            throw new IllegalArgumentException(format("Expected both 'from' (=%s) and 'until' (=%s) to be in 24-hour format", fromHour, untilHour));
        }
        int marginAfter = untilHour - fromHour;
        if (marginAfter <= 0) {
            throw new IllegalArgumentException("Expected the hours provided to be within the same day. Use using() and provide own margins instead.");
        }
        return CarbonAwareScheduleMargin.after(Duration.ofHours(marginAfter)).toScheduleExpression(Cron.daily(fromHour));
    }

    /**
     * Allows to relax the daily schedule of a {@link RecurringJob} to minimize carbon impact.
     * The {@link RecurringJob} may run daily before the hour {@code until} (in 24-hour format).
     *
     * @param untilHour the hour before which the {@link RecurringJob} must be scheduled (in 24-hour format).
     * @return A carbon aware schedule expression.
     */
    public static String dailyBefore(int untilHour) {
        return dailyBetween(0, untilHour);
    }
}
