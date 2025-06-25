package org.jobrunr.scheduling.carbonaware;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;

import static java.lang.String.format;
import static java.time.Instant.now;
import static org.jobrunr.utils.InstantUtils.toInstant;


/**
 * Utility class for everything related to Carbon-Aware Scheduling.
 * <p>
 * For scheduling jobs, you can use:
 * <ul>
 *     <li>{@link CarbonAware#before(Temporal)}</li>
 *     <li>{@link CarbonAware#between(Temporal, Temporal)}</li>
 *     <li>{@link CarbonAware#at(Temporal, TemporalAmount)}</li>
 * </ul>
 * <p>
 * For recurring jobs, you can use:
 * <ul>
 *     <li>{@link CarbonAware#cron(String, Duration)}</li>
 *     <li>{@link CarbonAware#interval(Duration, Duration)}</li>
 *     <li>{@link CarbonAware#dailyBefore(int)}</li>
 *     <li>{@link CarbonAware#dailyBetween(int, int)}</li>
 * </ul>
 */
public class CarbonAware {

    /**
     * Allows relaxing the scheduling of a job to minimize carbon impact.
     * The job will run before the provided {@code to} {@link Temporal} instance.
     * <p>
     * Supported {@link Temporal} implementations: {@link Instant}, {@link ChronoLocalDateTime}, {@link ChronoZonedDateTime}, {@link OffsetDateTime}.
     *
     * @param to the time, expressed as a {@link Temporal}, before which the job must be scheduled.
     * @return a carbon-aware period between {@code Instant.now()} and the provided {@code to}.
     */
    public static CarbonAwarePeriod before(Temporal to) {
        return between(now(), to);
    }

    /**
     * Allows relaxing the scheduling of a job to minimize carbon impact.
     * The job will run within the interval defined by the provided {@code from} and {@code to} {@link Temporal} instances.
     * <p>
     * Supported {@link Temporal} implementations: {@link Instant}, {@link ChronoLocalDateTime}, {@link ChronoZonedDateTime}, {@link OffsetDateTime}.
     *
     * @param from the start time of the carbon-aware margin, expressed as a {@link Temporal}.
     * @param to   the end time of the carbon-aware margin, expressed as a {@link Temporal}.
     * @return a carbon-aware period between the provided {@code from} and {@code to}.
     */
    public static CarbonAwarePeriod between(Temporal from, Temporal to) {
        return new CarbonAwarePeriod(toInstant(from), toInstant(to));
    }

    /**
     * Allows relaxing the scheduling of a job to minimize carbon impact.
     * The job is intended to run near the provided {@code at} {@link Temporal} instance, but may run up to {@code marginBeforeAndAfter} earlier or later.
     * <p>
     * Supported {@link Temporal} implementations: {@link Instant}, {@link ChronoLocalDateTime}, {@link ChronoZonedDateTime}, {@link OffsetDateTime}.
     *
     * @param at                   the central time, expressed as a {@link Temporal}, around which the job may be scheduled.
     * @param marginBeforeAndAfter the maximum time deviation before and after {@code at}, expressed as a {@link TemporalAmount}.
     * @return a carbon-aware period centered around {@code at}, extended by {@code marginBeforeAndAfter} in both directions.
     */
    public static CarbonAwarePeriod at(Temporal at, TemporalAmount marginBeforeAndAfter) {
        return between(at.minus(marginBeforeAndAfter), at.plus(marginBeforeAndAfter));
    }

    /**
     * Allows relaxing the scheduling of a job to minimize carbon impact.
     * The job is intended to run near the provided {@code at} {@link Temporal} instance, but may run up to {@code marginBefore} earlier
     * and {@code marginAfter} later.
     * <p>
     * Supported {@link Temporal} implementations: {@link Instant}, {@link ChronoLocalDateTime}, {@link ChronoZonedDateTime}, {@link OffsetDateTime}.
     *
     * @param at           the central time, expressed as a {@link Temporal}, around which the job may be scheduled.
     * @param marginBefore the maximum time the job may run before {@code at}, expressed as a {@link TemporalAmount}.
     * @param marginAfter  the maximum time the job may run after {@code at}, expressed as a {@link TemporalAmount}.
     * @return a carbon-aware period starting {@code marginBefore} before {@code at} and ending {@code marginAfter} after {@code at}.
     */
    public static CarbonAwarePeriod at(Temporal at, TemporalAmount marginBefore, TemporalAmount marginAfter) {
        return between(at.minus(marginBefore), at.plus(marginAfter));
    }

    /**
     * Allows to relax the schedule of a {@link RecurringJob} to minimize carbon impact.
     * The RecurringJob may run {@code marginBeforeAndAfter} earlier and {@code marginBeforeAndAfter} later than originally planned.
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
