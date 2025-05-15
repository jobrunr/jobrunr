package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.CarbonAwareScheduleMargin;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;

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
        this(carbonAwarePeriod.getFrom(), carbonAwarePeriod.getTo());
    }

    public CarbonAwareAwaitingState(Instant from, Instant to) {
        this(null, from, to, null);
    }

    public CarbonAwareAwaitingState(Instant preferredInstant, Instant from, Instant to, Instant createdAt, String reason) {
        // for json deserialization
        super(StateName.AWAITING, createdAt);
        this.preferredInstant = preferredInstant;
        this.from = from;
        this.to = to;
        this.reason = reason;
        validateCarbonAwarePeriod(from, to);
    }

    public CarbonAwareAwaitingState(Instant preferredInstant, Instant from, Instant to, String reason) {
        this(preferredInstant, from, to, Instant.now(), reason);
    }

    public CarbonAwareAwaitingState(Instant preferredInstant, CarbonAwareScheduleMargin margin, String reason) {
        this(preferredInstant, preferredInstant.minus(margin.getMarginBefore()), preferredInstant.plus(margin.getMarginAfter()), Instant.now(), reason);
    }

    public CarbonAwareAwaitingState(Instant preferredInstant, Instant from, Instant to) {
        this(preferredInstant, from, to, null);
    }

    public static JobState fromRecurringJob(CarbonAwareScheduleMargin margin, Instant scheduleAt, RecurringJob recurringJob) {
        return new CarbonAwareAwaitingState(scheduleAt, margin, "Awaiting by recurring job '" + recurringJob.getJobName() + "'");
    }

    public static JobState fromRecurringJobAheadOfTime(CarbonAwareScheduleMargin margin, Instant scheduleAt, RecurringJob recurringJob) {
        return new CarbonAwareAwaitingState(scheduleAt, margin, "Awaiting ahead of time by recurring job '" + recurringJob.getJobName() + "'");
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
        // why: this acts as a deadline for the awaiting state interval.
        // PreferredInstant can be null if not triggered from a recurring job.
        return to;
    }
}
