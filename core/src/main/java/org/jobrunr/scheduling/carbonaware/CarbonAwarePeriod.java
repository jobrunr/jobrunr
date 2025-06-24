package org.jobrunr.scheduling.carbonaware;

import org.jobrunr.utils.InstantUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

import static java.time.Instant.now;
import static org.jobrunr.utils.InstantUtils.toInstant;

/**
 * Represents a period of time, in which a job will be scheduled in a moment of low carbon emissions.
 */
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

    // TODO add javadoc
    // TODO add test
    public static CarbonAwarePeriod before(Temporal to) {
        return between(now(), to);
    }

    // TODO add javadoc
    // TODO add test
    public static CarbonAwarePeriod between(Temporal from, Temporal to) {
        return new CarbonAwarePeriod(toInstant(from), toInstant(to));
    }

}
