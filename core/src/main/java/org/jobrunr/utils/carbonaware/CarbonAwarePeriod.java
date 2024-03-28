package org.jobrunr.utils.carbonaware;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static java.time.Instant.now;

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
        Instant from = now().minusSeconds(1); // why: as otherwise the to is before the now()
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod beforeStartOf(LocalDate date) {
        Instant from = now().minusSeconds(1); // why: as otherwise the to is before the now()
        Instant to = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod beforeEnd(LocalDate date) {
        Instant from = now().minusSeconds(1); // why: as otherwise the to is before the now()
        Instant to = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod between(Instant from, Instant to) {
        return new CarbonAwarePeriod(from, to);
    }
}
