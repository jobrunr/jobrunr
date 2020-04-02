package org.jobrunr.jobs.states;

import java.time.Instant;

public class DeletedState extends AbstractJobState {

    public DeletedState() {
        super(StateName.DELETED);
    }

    public Instant getDeletedAt() {
        return getCreatedAt();
    }
}
