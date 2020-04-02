package org.jobrunr.jobs.states;

import java.time.Instant;

public class EnqueuedState extends AbstractJobState {

    public EnqueuedState() {
        super(StateName.ENQUEUED);
    }

    public Instant getEnqueuedAt() {
        return getCreatedAt();
    }
}
