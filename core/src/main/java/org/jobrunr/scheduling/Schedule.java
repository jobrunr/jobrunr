package org.jobrunr.scheduling;

import org.jobrunr.utils.annotations.VisibleFor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static java.time.Instant.now;

public abstract class Schedule implements Comparable<Schedule> {

    public static final int SMALLEST_SCHEDULE_IN_SECONDS = 5;


    /**
     * Calculates the next occurrence based on the creation time and the current time.
     *
     * @param createdAt Instant object when the schedule was first created
     * @param zoneId the zone for which to calculate the schedule
     * @return Instant of the next occurrence.
     */
    public Instant next(Instant createdAt, ZoneId zoneId) {
        return next(createdAt, now(), zoneId);
    }

    /**
     * Calculates the next occurrence based on the creation time and the provided base time.
     *
     * @param createdAtInstant Instant object when the schedule was first created
     * @param currentInstant Instant object used to calculate next occurrence (normally Instant.now()).
     * @param zoneId the zone for which to calculate the schedule
     * @return Instant of the next occurrence.
     */
    @VisibleFor("testing")
    public abstract Instant next(Instant createdAtInstant, Instant currentInstant, ZoneId zoneId);

    public abstract void validateSchedule();

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
}
