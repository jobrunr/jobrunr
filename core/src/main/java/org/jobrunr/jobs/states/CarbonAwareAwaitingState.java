package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;

import java.time.Instant;

import static java.lang.String.format;
import static java.time.Instant.now;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class CarbonAwareAwaitingState extends AbstractJobState implements Schedulable {
    private Instant preferredInstant;
    private Instant from;
    private Instant to;

    protected CarbonAwareAwaitingState() { // for json deserialization
        super(StateName.AWAITING);
        this.preferredInstant = null;
        this.from = null;
        this.to = null;
    }

    public CarbonAwareAwaitingState(CarbonAwarePeriod carbonAwarePeriod) {
        this(carbonAwarePeriod.getFrom(), carbonAwarePeriod.getTo());
    }

    public CarbonAwareAwaitingState(Instant to) {
        this(now(), to);
    }

    public CarbonAwareAwaitingState(Instant from, Instant to) {
        this(null, from, to);
    }

    public CarbonAwareAwaitingState(Instant preferredInstant, Instant from, Instant to, Instant createdAt) {
        // for json deserialization
        super(StateName.AWAITING, createdAt);
        this.preferredInstant = preferredInstant;
        this.from = from;
        this.to = to;
        validateCarbonAwarePeriod(from, to);
    }

    public CarbonAwareAwaitingState(Instant preferredInstant, Instant from, Instant to) {
        this(preferredInstant, from, to, Instant.now());
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

    public void moveToNextState(Job job, Instant idealMoment) {
        moveToNextState(job, idealMoment, "Job scheduled at " + idealMoment + " to minimize carbon impact.");
    }

    public void moveToNextState(Job job, Instant idealMoment, String reason) {
        if (!(job.getJobState() instanceof CarbonAwareAwaitingState)) {
            throw new IllegalStateException("Only jobs in CarbonAwaitingState can move to a next state");
        }
        job.scheduleAt(idealMoment, reason);
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
        return preferredInstant == null ? from : preferredInstant;
    }
}
