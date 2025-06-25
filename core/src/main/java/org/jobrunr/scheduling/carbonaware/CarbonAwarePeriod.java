package org.jobrunr.scheduling.carbonaware;

import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

import static java.time.Instant.now;
import static org.jobrunr.utils.InstantUtils.toInstant;

/**
 * Represents a period of time, in which a job will be scheduled in a moment of low carbon emissions.
 *
 * <em>NOTE: CarbonAwarePeriod implements {@link Temporal} a marker interface and all temporal related methods throw Exceptions for now </em>
 */
public class CarbonAwarePeriod implements Temporal {

    private final Instant from;
    private final Instant to;

    CarbonAwarePeriod(Instant from, Instant to) {
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
     * The job will run between the two provided {@code from} and {@code to} Temporal instances as the interval.
     *
     * @param from the start time expressed in java.time.Temporal of the carbon aware margin.
     * @param to   the end time expressed in java.time.Temporal of the carbon aware margin.
     * @return A carbon aware period between the provided {@code from} and {@code to}.
     */
    public static CarbonAwarePeriod between(Temporal from, Temporal to) {
        return new CarbonAwarePeriod(toInstant(from), toInstant(to));
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        throw new UnsupportedOperationException("isSupported is not supported by CarbonAwarePeriod");
    }

    @Override
    public Temporal with(TemporalField field, long newValue) {
        throw new UnsupportedOperationException("with is not supported by CarbonAwarePeriod");
    }

    @Override
    public Temporal plus(long amountToAdd, TemporalUnit unit) {
        throw new UnsupportedOperationException("plus is not supported by CarbonAwarePeriod");
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        throw new UnsupportedOperationException("until is not supported by CarbonAwarePeriod");
    }

    @Override
    public boolean isSupported(TemporalField field) {
        throw new UnsupportedOperationException("isSupported is not supported by CarbonAwarePeriod");
    }

    @Override
    public long getLong(TemporalField field) {
        throw new UnsupportedOperationException("getLong is not supported by CarbonAwarePeriod");
    }
}
