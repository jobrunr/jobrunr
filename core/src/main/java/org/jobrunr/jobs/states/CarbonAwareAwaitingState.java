package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;
import org.jobrunr.utils.carbonaware.CarbonAwarePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static java.time.Duration.ofHours;
import static java.time.Instant.now;

public class CarbonAwareAwaitingState extends AbstractJobState {
    private final Instant from;
    private final Instant to;
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareAwaitingState.class);

    protected CarbonAwareAwaitingState() { // for json deserialization
        super(StateName.AWAITING);
        this.from = null;
        this.to = null;
    }

    public CarbonAwareAwaitingState(CarbonAwarePeriod when) {
        this(when.getFrom(), when.getTo());
    }

    public CarbonAwareAwaitingState(Instant to) {
        this(now(), to);
    }

    public CarbonAwareAwaitingState(Instant from, Instant to) {
        super(StateName.AWAITING);
        this.from = from;
        this.to = to;
        validateCarbonAwarePeriod(from, to);
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
        if (job.getJobState().getName() != StateName.AWAITING) {
            throw new IllegalStateException("Only jobs in AWAITING can move to a next state");
        }
        Instant now = now();
        if (!idealMoment.isAfter(now)) {
            LOGGER.warn("Schedule job {} immediately, as we don't have data", job.getId());
            job.enqueue();
        } else {
            job.scheduleAt(idealMoment, reason);
        }
    }

    public static void validateCarbonAwarePeriod(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        } else if (to.isBefore(now().plus(ofHours(3)))) {
            throw new IllegalArgumentException("'to' must be at least 3 hours in the future to use Carbon Aware Scheduling");
        } //TODO: review the "3-hour" rule
    }
}
