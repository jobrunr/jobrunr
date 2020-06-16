package org.jobrunr.jobs.states;

import java.time.Instant;

import static org.jobrunr.jobs.states.StateName.ENQUEUED;

public class EnqueuedState extends AbstractJobState {

    public EnqueuedState() {
        super(ENQUEUED);
    }

    public Instant getEnqueuedAt() {
        return getCreatedAt();
    }
}
