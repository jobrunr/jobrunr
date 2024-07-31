package org.jobrunr.scheduling.carbonaware;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.Schedule.CarbonAwareScheduleMargin;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;

import java.time.Duration;

import static java.lang.String.format;

public class CarbonAware {

    /**
     * Allows to relax the schedule of a {@link RecurringJob} to minimize carbon impact.
     * The RecurringJob may run {@code marginBefore} earlier and {@code marginAfter} later than originally planned.
     *
     * @param scheduleExpression the scheduleExpression may be a {@link CronExpression} or a Duration (for {@link Interval} scheduling).
     * @param marginBefore       the amount of time the {@link RecurringJob} is allowed to run before its original schedule.
     * @param marginAfter        the amount of time the {@link RecurringJob} is allowed to run after its original schedule.
     * @return A carbon aware schedule expression.
     */
    public static String using(String scheduleExpression, Duration marginBefore, Duration marginAfter) {
        return CarbonAwareScheduleMargin.margin(marginBefore, marginAfter).toScheduleExpression(scheduleExpression);
    }

    /**
     * Allows to relax the {@link Interval} schedule of a {@link RecurringJob} to minimize carbon impact.
     * The RecurringJob may run {@code marginBefore} earlier and {@code marginAfter} later than original planned.
     *
     * @param scheduleExpression the scheduleExpression, a Duration for {@link Interval} scheduling.
     * @param marginBefore       the amount of time the {@link RecurringJob} is allowed to run before its original schedule.
     * @param marginAfter        the amount of time the {@link RecurringJob} is allowed to run after its original schedule.
     * @return A carbon aware {@link Interval} schedule expression.
     */
    public static String using(Duration scheduleExpression, Duration marginBefore, Duration marginAfter) {
        return using(scheduleExpression.toString(), marginBefore, marginAfter);
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
