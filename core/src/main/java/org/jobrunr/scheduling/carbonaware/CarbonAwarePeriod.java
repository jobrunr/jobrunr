package org.jobrunr.scheduling.carbonaware;

import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

import static java.time.Instant.now;
import static org.jobrunr.utils.InstantUtils.toInstant;

/**
 * Represents a period of time in which a job may be scheduled to minimize carbon emissions.
 * <p>
 * A {@code CarbonAwarePeriod} is constructed using the {@link CarbonAware} utility class. It defines a flexible scheduling window
 * that can be centered around a point in time, start from now until a deadline, or be bounded by a custom range.
 * The actual execution time within this window is determined by a carbon-aware scheduler.
 * <p>
 * <strong>Note:</strong> The scheduling window must currently span at least <strong>3 hours</strong>. This is due to the
 * underlying carbon intensity data being available only at an hourly resolution.
 * <p>
 * Example usages:
 * <pre>{@code
 * // Schedule before a specific deadline
 * CarbonAwarePeriod beforeDeadline = CarbonAware.before(Instant.parse("2025-07-01T08:00:00Z"));
 *
 * // Schedule between two custom times
 * CarbonAwarePeriod betweenTimes = CarbonAware.between(
 *     Instant.parse("2025-07-01T00:00:00Z"),
 *     Instant.parse("2025-07-01T08:00:00Z")
 * );
 *
 * // Schedule around a target time with flexibility
 * CarbonAwarePeriod flexible = CarbonAware.at(
 *     Instant.parse("2025-07-01T04:00:00Z"),
 *     Duration.ofHours(4)
 * );
 * }</pre>
 * <p>
 * <em>NOTE:</em> {@code CarbonAwarePeriod} implements {@link Temporal} as a marker interface. All temporal methods
 * currently throw {@link UnsupportedOperationException} and are not intended for direct time manipulation.
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
     * Allows to relax the scheduling of a job to minimize carbon impact.
     * The job will run before the provided {@code to} Temporal instance.
     *
     * @param to the time expressed in java.time.Temporal before which the job must be scheduled.
     * @return A carbon aware period between {@code Instant.now()} and the provided {@code to}.
     */
    public static CarbonAwarePeriod before(Temporal to) {
        return between(now(), to);
    }

    /**
     * Allows to relax the scheduling of a job to minimize carbon impact.
     * The job will run between the two provided {@code from} and {@code to} Temporal instances as the interval.
     *
     * @param from the start time expressed in {@link Temporal} of the carbon aware margin.
     * @param to   the end time expressed in {@link Temporal} of the carbon aware margin.
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
