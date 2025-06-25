package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.scheduling.carbonaware.CarbonAwareScheduleMargin;

import java.time.Duration;
import java.time.Instant;

import static java.lang.String.format;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class CarbonAwareAwaitingState extends AbstractJobState implements SchedulableState {
    private Instant preferredInstant;
    private Instant from;
    private Instant to;
    private String reason;

    protected CarbonAwareAwaitingState() { // for json deserialization
        super(StateName.AWAITING);
        this.preferredInstant = null;
        this.from = null;
        this.to = null;
    }

    public CarbonAwareAwaitingState(CarbonAwarePeriod carbonAwarePeriod) {
        this(null, carbonAwarePeriod.getFrom(), carbonAwarePeriod.getTo(), null);
    }

    public CarbonAwareAwaitingState(Instant preferredInstant, CarbonAwareScheduleMargin margin, String reason) {
        this(preferredInstant, preferredInstant.minus(margin.getMarginBefore()), preferredInstant.plus(margin.getMarginAfter()), reason);
    }

    public CarbonAwareAwaitingState(Instant preferredInstant, Instant from, Instant to, String reason) {
        this(preferredInstant, from, to, reason, Instant.now());
    }

    public CarbonAwareAwaitingState(Instant preferredInstant, Instant from, Instant to, String reason, Instant createdAt) {
        super(StateName.AWAITING, createdAt);
        this.preferredInstant = preferredInstant;
        this.from = from;
        this.to = to;
        this.reason = reason;
        validateCarbonAwarePeriod(from, to);
    }

    public Duration getMarginDuration() {
        return Duration.between(from, to);
    }

    public Instant getFallbackInstant() {
        return preferredInstant != null ? preferredInstant : from;
    }

    public Instant getPreferredInstant() {
        return preferredInstant;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    public CarbonAwarePeriod getPeriod() {
        return CarbonAwarePeriod.between(from, to);
    }

    public String getReason() {
        return reason;
    }

    public void moveToNextState(Job job, Instant scheduleAt, String reason) {
        if (!(job.getJobState() instanceof CarbonAwareAwaitingState)) {
            throw new IllegalStateException("Only jobs in CarbonAwaitingState can move to a next state");
        }
        job.scheduleAt(scheduleAt, reason);
    }

    private static void validateCarbonAwarePeriod(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(format("'from' (=%s) and 'to' (=%s) must be non-null", from, to));
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(format("'from' (=%s) must be before 'to' (=%s)", from, to));
        }
    }

    @Override
    public String toString() {
        return "CarbonAwareAwaitingState{" +
                "preferredInstant=" + preferredInstant +
                ", from=" + from +
                ", to=" + to +
                '}';
    }

    @Override
    public Instant getScheduledAt() {
        // why: this acts as a deadline for the awaiting state interval.
        // PreferredInstant can be null if not triggered from a recurring job.
        return to;
    }
}
