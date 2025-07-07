package org.jobrunr.jobs.states;

import java.time.Instant;

import static org.jobrunr.jobs.states.StateName.ENQUEUED;

public class EnqueuedState extends AbstractJobState {

    public EnqueuedState() {
        super(ENQUEUED);
    }

    public EnqueuedState(Instant createdAt) {
        super(ENQUEUED, createdAt);
    }

    public Instant getEnqueuedAt() {
        return getCreatedAt();
    }
}
