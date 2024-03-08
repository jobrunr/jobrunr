package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;

import java.time.Instant;

public class CarbonAwareAwaitingState extends AbstractJobState {
    private final Instant deadline;

    protected CarbonAwareAwaitingState() { // for json deserialization
        this(null);
    }

    protected CarbonAwareAwaitingState(Instant deadline) {
        super(StateName.AWAITING);
        this.deadline = deadline;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void moveToNextState(Job job, Instant idealMoment) {
        moveToNextState(job, idealMoment, "Job scheduled at " + idealMoment + " to minimize carbon impact.");
    }

    public void moveToNextState(Job job, Instant idealMoment, String reason) {
        if (job.getJobState() != this) {
            throw new IllegalStateException("Only jobs in AWAITING state can move to a next state");
        }
        throw new UnsupportedOperationException("Implement me");
    }
}
