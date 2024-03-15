package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;
import org.jobrunr.utils.carbonaware.CarbonAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class CarbonAwareAwaitingState extends AbstractJobState {
    private final Instant from;
    private final Instant to;
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareAwaitingState.class);

    protected CarbonAwareAwaitingState() { // for json deserialization
        this(null);
    }

    public CarbonAwareAwaitingState(CarbonAware when) {
        this(when.getFrom(), when.getTo());
    }

    public CarbonAwareAwaitingState(Instant from, Instant to) {
        super(StateName.AWAITING);
        this.from =from;
        this.to =to;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    @Deprecated
    public Instant getDeadline() {
        return to;
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
            // job.scheduleAt(now, "Schedule immediately, as we don't have data");
        } else {
            job.scheduleAt(idealMoment, reason);
        }
    }
}
