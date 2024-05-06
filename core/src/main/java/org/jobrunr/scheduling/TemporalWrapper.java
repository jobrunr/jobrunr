package org.jobrunr.scheduling;

import org.jobrunr.utils.carbonaware.CarbonAwarePeriod;

import java.time.Instant;

public class TemporalWrapper {
    private final Instant instant;
    private final CarbonAwarePeriod carbonAwarePeriod;

    private TemporalWrapper(Instant instant, CarbonAwarePeriod carbonAwarePeriod) {
        this.instant = instant;
        this.carbonAwarePeriod = carbonAwarePeriod;
    }

    public static TemporalWrapper ofInstant(Instant instant) {
        return new TemporalWrapper(instant, null);
    }

    public static TemporalWrapper ofCarbonAwarePeriod(CarbonAwarePeriod period) {
        return new TemporalWrapper(null, period);
    }

    public boolean isInstant() {
        return instant != null;
    }

    public boolean isCarbonAwarePeriod() {
        return carbonAwarePeriod != null;
    }

    public Instant getInstant() {
        return instant;
    }

    public CarbonAwarePeriod getCarbonAwarePeriod() {
        return carbonAwarePeriod;
    }

    public String getType() {
        return isInstant() ? "Instant" : "CarbonAwarePeriod";
    }
}