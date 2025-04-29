package org.jobrunr.jobs.states;

import java.time.Duration;
import java.time.Instant;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class SucceededState extends AbstractJobState {

    private Duration latencyDuration;
    private Duration processDuration;

    protected SucceededState() { // for json deserialization
        this(null, null);
    }

    public SucceededState(Duration latencyDuration, Duration processDuration) {
        this(latencyDuration, processDuration, Instant.now());
    }

    public SucceededState(Duration latencyDuration, Duration processDuration, Instant createdAt) {
        super(StateName.SUCCEEDED, createdAt);
        this.latencyDuration = latencyDuration;
        this.processDuration = processDuration;
    }

    public Duration getLatencyDuration() {
        return latencyDuration;
    }

    public Duration getProcessDuration() {
        return processDuration;
    }
}
