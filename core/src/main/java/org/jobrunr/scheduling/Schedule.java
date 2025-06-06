package org.jobrunr.scheduling;

import org.jobrunr.utils.annotations.VisibleFor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.StringUtils.substringBeforeLast;

public abstract class Schedule implements Comparable<Schedule> {
    private final String expression;
    private CarbonAwareScheduleMargin carbonAwareScheduleMargin;

    protected Schedule() {
        this.expression = null;
    }

    protected Schedule(String scheduleWithOptionalCarbonAwareScheduleMargin) {
        if (isNullOrEmpty(scheduleWithOptionalCarbonAwareScheduleMargin)) {
            throw new IllegalArgumentException("Expected scheduleWithOptionalCarbonAwareScheduleMargin to be non-null and non-empty.");
        }
        this.carbonAwareScheduleMargin = CarbonAwareScheduleMargin.parse(scheduleWithOptionalCarbonAwareScheduleMargin);
        this.expression = this.carbonAwareScheduleMargin == null
                ? scheduleWithOptionalCarbonAwareScheduleMargin
                : substringBeforeLast(scheduleWithOptionalCarbonAwareScheduleMargin, CarbonAwareScheduleMargin.MARGIN_OPENING_TAG).trim();
    }

    /**
     * Calculates the next occurrence based on the creation time and the current time.
     *
     * @param createdAt Instant object when the schedule was first created
     * @param zoneId    the zone for which to calculate the schedule
     * @return Instant of the next occurrence.
     */
    public Instant next(Instant createdAt, ZoneId zoneId) {
        return next(createdAt, now(), zoneId);
    }

    /**
     * Calculates the next occurrence based on the creation time and the provided base time.
     *
     * @param createdAtInstant Instant object when the schedule was first created
     * @param currentInstant   Instant object used to calculate next occurrence (normally Instant.now()).
     * @param zoneId           the zone for which to calculate the schedule
     * @return Instant of the next occurrence.
     */
    @VisibleFor("testing")
    public abstract Instant next(Instant createdAtInstant, Instant currentInstant, ZoneId zoneId);

    public String getExpression() {
        return expression;
    }

    public CarbonAwareScheduleMargin getCarbonAwareScheduleMargin() {
        return carbonAwareScheduleMargin;
    }

    public boolean isCarbonAware() {
        return carbonAwareScheduleMargin != null;
    }

    public boolean isNotCarbonAware() {
        return !isCarbonAware();
    }

    public final Duration durationBetweenSchedules() {
        Instant base = Instant.EPOCH.plusSeconds(3600);
        Instant run1 = this.next(base, base, ZoneOffset.UTC);
        Instant run2 = this.next(base, run1, ZoneOffset.UTC);
        return between(run1, run2);
    }

    public void validate() {
        if (isNotCarbonAware()) return;

        Duration durationBetweenSchedules = durationBetweenSchedules();
        Duration totalMargin = carbonAwareScheduleMargin.getMarginBefore().plus(carbonAwareScheduleMargin.getMarginAfter());

        if (durationBetweenSchedules.minus(totalMargin).isNegative()) {
            throw new IllegalStateException("The total carbon aware margin must be lower than the duration between each schedule.");
        }
    }

    /**
     * Compare two {@code Schedule} objects based on next occurrence.
     * <p>
     * The next occurrences are calculated based on the current time.
     *
     * @param schedule the {@code Schedule} to be compared.
     * @return the value {@code 0} if this {@code Schedule} next occurrence is equal
     * to the argument {@code Schedule} next occurrence; a value less than
     * {@code 0} if this {@code Schedule} next occurrence is before the
     * argument {@code Schedule} next occurrence; and a value greater than
     * {@code 0} if this {@code Schedule} next occurrence is after the
     * argument {@code Schedule} next occurrence.
     */
    @Override
    public int compareTo(Schedule schedule) {
        if (schedule == this) {
            return 0;
        }

        Instant baseInstant = now();
        final Instant nextAnother = schedule.next(baseInstant, ZoneOffset.UTC);
        final Instant nextThis = this.next(baseInstant, ZoneOffset.UTC);

        return nextThis.compareTo(nextAnother);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return isCarbonAware() ? carbonAwareScheduleMargin.toScheduleExpression(expression) : expression;
    }

}
