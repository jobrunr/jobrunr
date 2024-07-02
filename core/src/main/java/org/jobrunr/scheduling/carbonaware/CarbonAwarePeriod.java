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
        Instant from = now().minusSeconds(1); // why: as otherwise the to is before the now()
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod between(LocalDate from, LocalDate to) {
        return new CarbonAwarePeriod(from.atStartOfDay(ZoneId.systemDefault()).toInstant(), to.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static CarbonAwarePeriod before(LocalDate date) {
        Instant from = now().minusSeconds(1); // why: as otherwise the to is before the now()
        Instant to = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return new CarbonAwarePeriod(from, to);
    }

    public static CarbonAwarePeriod between(LocalDateTime from, LocalDateTime to) {
        return new CarbonAwarePeriod(from.atZone(ZoneId.systemDefault()).toInstant(), to.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static CarbonAwarePeriod before(LocalDateTime dateTime) {
        return new CarbonAwarePeriod(now().minusSeconds(1), dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static CarbonAwarePeriod between(String from, String to) {
        return new CarbonAwarePeriod(Instant.parse(from), Instant.parse(to));
    }

    public static CarbonAwarePeriod before(String dateTime) {
        return new CarbonAwarePeriod(now().minusSeconds(1), Instant.parse(dateTime));
    }

    public static CarbonAwarePeriod between(ZonedDateTime from, ZonedDateTime to) {
        return new CarbonAwarePeriod(from.toInstant(), to.toInstant());
    }

    public static CarbonAwarePeriod before(ZonedDateTime dateTime) {
        return new CarbonAwarePeriod(now().minusSeconds(1), dateTime.toInstant());
    }
}
