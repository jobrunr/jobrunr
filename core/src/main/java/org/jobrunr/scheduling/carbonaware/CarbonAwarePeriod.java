package org.jobrunr.scheduling.carbonaware;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.utils.InstantUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

import static java.time.Instant.now;
import static org.jobrunr.utils.InstantUtils.toInstant;

/**
 * Represents a period of time, in which a job will be scheduled in a moment of low carbon emissions.
 */
public class CarbonAwarePeriod {

    private final Instant from;
    private final Instant to;

    private CarbonAwarePeriod(Instant from, Instant to) {
        this.from = from;
        this.to = to;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

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
     * The job will run between the two provided {@code to} Temporal instances as the interval.
     *
     * @param from the start time expressed in java.time.Temporal of the carbon aware margin.
     * @param to the end time expressed in java.time.Temporal of the carbon aware margin.
     * @return A carbon aware period between the provided {@code from} and {@code to}.
     */
    public static CarbonAwarePeriod between(Temporal from, Temporal to) {
        return new CarbonAwarePeriod(toInstant(from), toInstant(to));
    }

}
