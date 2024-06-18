package org.jobrunr.scheduling.carbonaware;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Represents a period of time, in which a job will be scheduled in a moment of low carbon emissions.
 * <p>
 * The CarbonAwarePeriod can be created in different ways:
 *     <ul>
 *         <li>between two {@link Instant}s</li>
 *         <li>before a {@link Instant}</li>
 *         <li>after a {@link Instant} (max 2 days after the given instant)</li>
 *         <li>between two {@link LocalDate}s</li>
 *         <li>before a {@link LocalDate}</li>
 *         <li>after a {@link LocalDate} (max 2 days after the given date)</li>
 *         <li>between two {@link LocalDateTime}s</li>
 *         <li>before a {@link LocalDateTime}</li>
 *         <li>after a {@link LocalDateTime} (max 2 days after the given date)</li>
 *         <li>between two {@link ZonedDateTime}s</li>
 *         <li>before a {@link ZonedDateTime}</li>
 *         <li>after a {@link ZonedDateTime}</li>
 *         <li>between two {@link String}s</li>
 *         <li>before a {@link String}</li>
 *         <li>until tomorrow</li>
 *         <li>today</li>
 *         <li>before next Sunday</li>
 *      </ul>
 * </p>
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

    public static CarbonAwarePeriod after(Instant from) {
        Instant to = from.plus(2, DAYS);
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

    public static CarbonAwarePeriod after(LocalDate date) {
        return new CarbonAwarePeriod(date.atStartOfDay(ZoneId.systemDefault()).toInstant(), date.plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant());
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

    public static CarbonAwarePeriod untilTomorrow() {
        return new CarbonAwarePeriod(now().minusSeconds(1), now().truncatedTo(DAYS).plus(1, DAYS));
    }

    public static CarbonAwarePeriod today() {
        Instant to = now().truncatedTo(DAYS).plus(1, DAYS).minusNanos(1);
        return new CarbonAwarePeriod(now().minusSeconds(1), to);
    }

    public static CarbonAwarePeriod beforeNextSunday() {
        LocalDate nextSunday = LocalDate.now(ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        return new CarbonAwarePeriod(now().minusSeconds(1), nextSunday.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
