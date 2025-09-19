package org.jobrunr.jobs.states;

import java.time.Instant;

import static java.util.Optional.ofNullable;

@SuppressWarnings({"CanBeFinal", "FieldMayBeFinal"}) // because of JSON-B
public abstract class AbstractJobState implements JobState {

    private final StateName state;
    private Instant createdAt;

    protected AbstractJobState(StateName state) {
        this(state, Instant.now());
    }

    protected AbstractJobState(StateName state, Instant createdAt) {
        this.state = state;
        this.createdAt = ofNullable(createdAt).orElse(Instant.now());
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
