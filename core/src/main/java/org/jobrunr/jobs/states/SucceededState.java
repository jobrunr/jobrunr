package org.jobrunr.jobs.states;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class SucceededState extends AbstractJobState {

    private Duration latencyDuration;
    private Duration processDuration;
    private BigDecimal spotPrice;
    private BigDecimal instancePrice;

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

        // TODO can we pass these as parameters instead, from the BackgroundJobPerformer on job.succeeded call?
        Map<String, String> environment = System.getenv();
        if (environment.get("JOBRUNR_COST_AWARE_INSTANCE_PRICE") != null && environment.get("JOBRUNR_COST_AWARE_SPOT_PRICE") != null) {
            instancePrice = new BigDecimal(environment.get("JOBRUNR_COST_AWARE_INSTANCE_PRICE"));
            spotPrice = new BigDecimal(environment.get("JOBRUNR_COST_AWARE_SPOT_PRICE"));
        }
    }

    public Duration getLatencyDuration() {
        return latencyDuration;
    }

    public Duration getProcessDuration() {
        return processDuration;
    }

    public BigDecimal getSpotPrice() {
        return spotPrice;
    }

    public BigDecimal getInstancePrice() {
        return instancePrice;
    }
}
