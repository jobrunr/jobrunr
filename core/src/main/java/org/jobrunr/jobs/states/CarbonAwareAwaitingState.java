package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;

import java.time.Instant;

public class CarbonAwareAwaitingState extends AbstractJobState {
    private final Instant deadline;

    protected CarbonAwareAwaitingState() { // for json deserialization
        this(null);
    }

    public CarbonAwareAwaitingState(Instant deadline) {
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
        if (job.getJobState().getName() != StateName.AWAITING) {
            throw new IllegalStateException("Only jobs in AWAITING can move to a next state");
        }
        Instant now = Instant.now();
        if (idealMoment.isBefore(now)) {
            job.enqueue();
        } else {
            job.scheduleAt(idealMoment, reason);
        }
    }
}
