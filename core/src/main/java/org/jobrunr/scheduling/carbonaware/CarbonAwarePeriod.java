package org.jobrunr.scheduling.carbonaware;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.Instant.now;

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

    public static CarbonAwarePeriod between(Instant from, Instant to) {
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod before(Instant to) {
        return between(now(), to);
    }

    public static CarbonAwarePeriod between(LocalDate from, LocalDate to) {
        return between(from.atStartOfDay(ZoneId.systemDefault()).toInstant(), to.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static CarbonAwarePeriod before(LocalDate date) {
        Instant to = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return before(to);
    }

    public static CarbonAwarePeriod between(LocalDateTime from, LocalDateTime to) {
        return between(from.atZone(ZoneId.systemDefault()).toInstant(), to.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static CarbonAwarePeriod before(LocalDateTime dateTime) {
        return before(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static CarbonAwarePeriod between(ZonedDateTime from, ZonedDateTime to) {
        return between(from.toInstant(), to.toInstant());
    }

    public static CarbonAwarePeriod before(ZonedDateTime dateTime) {
        return before(dateTime.toInstant());
    }
}
