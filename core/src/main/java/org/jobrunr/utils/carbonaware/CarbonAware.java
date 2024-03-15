package org.jobrunr.utils.carbonaware;

import java.time.Instant;

public class CarbonAware {

    private final Instant from;
    private final Instant to;

    private CarbonAware(Instant from, Instant to) {
        this.from = from;
        this.to = to;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    public static CarbonAware before(Instant to) {
        return new CarbonAware(Instant.now(), to);
    }

    public static CarbonAware between(Instant from, Instant to) {
        return new CarbonAware(from, to);
    }
}
