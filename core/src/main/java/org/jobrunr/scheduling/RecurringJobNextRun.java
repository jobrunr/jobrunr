package org.jobrunr.scheduling;

import org.jobrunr.utils.carbonaware.CarbonAwarePeriod;

import java.time.Instant;

/**
 * Wrapper class. Contains either an Instant or a CarbonAwarePeriod (not both). Used to determine the next run in recurring jobs
 */
public class RecurringJobNextRun {
    private final Instant instant;
    private final CarbonAwarePeriod carbonAwarePeriod;

    private RecurringJobNextRun(Instant instant, CarbonAwarePeriod carbonAwarePeriod) {
        this.instant = instant;
        this.carbonAwarePeriod = carbonAwarePeriod;
    }

    public static RecurringJobNextRun ofInstant(Instant instant) {
        return new RecurringJobNextRun(instant, null);
    }

    public static RecurringJobNextRun ofCarbonAwarePeriod(CarbonAwarePeriod period) {
        return new RecurringJobNextRun(null, period);
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

}