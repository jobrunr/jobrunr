package org.jobrunr.jobs.states;

import java.time.Duration;

public class SucceededState extends AbstractJobState {

    private final Duration latencyDuration;
    private final Duration processDuration;

    private SucceededState() { // for jackson deserialization
        this(null, null);
    }

    public SucceededState(Duration latencyDuration, Duration processDuration) {
        super(StateName.SUCCEEDED);
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
