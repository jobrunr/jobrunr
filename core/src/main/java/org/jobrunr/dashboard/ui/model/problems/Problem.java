package org.jobrunr.dashboard.ui.model.problems;

import java.time.Instant;

public abstract class Problem {

    public final String type;
    public final Instant createdAt;

    protected Problem(String type) {
        this(type, Instant.now());
    }

    protected Problem(String type, Instant createdAt) {
        this.type = type;
        this.createdAt = createdAt;
    }
}
