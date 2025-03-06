package org.jobrunr.jobs.states;

import java.time.Instant;

@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"}) // because of JSON-B
public abstract class AbstractJobState implements JobState {

    private final StateName state;
    private Instant createdAt;

    protected AbstractJobState(StateName state, Instant createdAt) {
        this.state = state;
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        this.createdAt = createdAt;
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
