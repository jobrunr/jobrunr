package org.jobrunr.scheduling.interval;

import org.jobrunr.scheduling.Schedule;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.jobrunr.utils.InstantUtils.isInstantAfterOrEqualTo;

public class Interval extends Schedule {

    private final Duration duration;

    public Interval(Duration duration) {
        super(duration.toString());
        this.duration = duration;
    }

    public Interval(String durationExpression) {
        super(durationExpression);
        this.duration = Duration.parse(getExpression());
    }

    @Override
    public Instant next(Instant createdAtInstant, Instant currentInstant, ZoneId zoneId) {
        if (isInstantAfterOrEqualTo(createdAtInstant, currentInstant)) return createdAtInstant.plus(duration);

        Duration durationUntilNow = Duration.between(createdAtInstant, currentInstant);
        long amountOfDurationsUntilNow = durationUntilNow.toNanos() / duration.toNanos();
        return createdAtInstant.plusNanos(duration.toNanos() * (amountOfDurationsUntilNow + 1));
    }

    /**
     * Compares this object against the specified object. The result is {@code true}
     * if and only if the argument is not {@code null} and is a {@code Schedule}
     * object that whose seconds, minutes, hours, days, months, and days of
     * weeks sets are equal to those of this schedule.
     * <p>
     * The expression string used to create the schedule is not considered, as two
     * different expressions may produce same schedules.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Interval))
            return false;
        if (this == obj)
            return true;

        Interval interval = (Interval) obj;

        return this.duration.equals(interval.duration);
    }

    @Override
    public int hashCode() {
        return duration.hashCode();
    }

}
