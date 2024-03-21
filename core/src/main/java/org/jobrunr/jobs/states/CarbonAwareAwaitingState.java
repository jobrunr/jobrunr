package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;
import org.jobrunr.utils.carbonaware.CarbonAwarePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

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
        this(Instant.now(), to);
    }

    public CarbonAwareAwaitingState(Instant from, Instant to) {
        super(StateName.AWAITING);
        this.from =from;
        this.to =to;
        validatePeriod(from, to);
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
        Instant now = Instant.now();
        if (!idealMoment.isAfter(now)) {
            LOGGER.warn("Schedule job {} immediately, as we don't have data", job.getId());
            job.enqueue();
        } else {
            job.scheduleAt(idealMoment, reason);
        }
    }

    private void validatePeriod(Instant from, Instant to) {
        long toleranceSeconds = 1;

        if (from.isAfter(to.plusSeconds(toleranceSeconds))) {
            throw new IllegalArgumentException("The 'from' date must be before the 'to' date");
        }
        if (to.isBefore(from.plusSeconds(3 * 60 * 60))) {
            throw new IllegalArgumentException("The 'to' date must be at least 3 hours after the 'from' date");
        }
        if (to.isBefore(Instant.now().minusSeconds(toleranceSeconds))) {
            throw new IllegalArgumentException("The 'to' date must be in the future");
        }
    }
}
