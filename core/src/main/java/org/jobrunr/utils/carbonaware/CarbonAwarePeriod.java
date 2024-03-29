package org.jobrunr.utils.carbonaware;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

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

    public static CarbonAwarePeriod between(Instant from, Instant to) {
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod between(String from, String to) {
        return new CarbonAwarePeriod(Instant.parse(from), Instant.parse(to));
    }

    public static CarbonAwarePeriod before(Instant to) {
        Instant from = now().minusSeconds(1); // why: as otherwise the to is before the now()
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod after(Instant from) {
        Instant to = from.plus(2, ChronoUnit.DAYS);
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod before(LocalDate date) {
        Instant from = now().minusSeconds(1); // why: as otherwise the to is before the now()
        Instant to = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod after(LocalDate date) {
        Instant from = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = date.plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return new CarbonAwarePeriod(from, to);
    }
}
