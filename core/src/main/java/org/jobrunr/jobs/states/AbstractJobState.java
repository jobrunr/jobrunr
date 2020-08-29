package org.jobrunr.jobs.states;

import java.time.Instant;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public abstract class AbstractJobState implements JobState {

    private final StateName state;
    private Instant createdAt;

    protected AbstractJobState(StateName state) {
        this.state = state;
        this.createdAt = Instant.now();
    }

    @Override
    public StateName getName() {
        return state;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return getCreatedAt();
    }
}
