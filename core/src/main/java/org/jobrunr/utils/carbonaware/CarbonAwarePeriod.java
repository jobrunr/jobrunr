package org.jobrunr.utils.carbonaware;

import java.time.Instant;

public class CarbonAwarePeriod {

    private final Instant from;
    private final Instant to;

    private CarbonAwarePeriod(Instant from, Instant to) {
        this.from = from;
        this.to = to;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    public static CarbonAwarePeriod before(Instant to) {
        return new CarbonAwarePeriod(Instant.now(), to);
    }

    public static CarbonAwarePeriod between(Instant from, Instant to) {
        return new CarbonAwarePeriod(from, to);
    }
}
