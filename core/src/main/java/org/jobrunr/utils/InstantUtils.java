package org.jobrunr.utils;

import javax.swing.text.html.Option;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InstantUtils {

    private InstantUtils() {
    }

    public static boolean isInstantInPeriod(Instant instant, Instant startOfPeriod, Instant endOfPeriod) {
        return isInstantAfterOrEqualTo(instant, startOfPeriod) && instant.isBefore(endOfPeriod);
    }

    public static Instant max(List<Instant> instants) {
        return instants.stream()
                .filter(i -> i != null)
                .max(Instant::compareTo)
                .orElse(null);
    }

    public static Instant max(Instant... instants) {
        return max(Arrays.asList(instants));
    }

    public static Instant max(Optional<Instant>... instants) {
        return max(Arrays.stream(instants)
                .filter(opt -> opt != null)
                .map(instant -> instant.orElse(null))
                .collect(Collectors.toList()));
    }

    public static boolean isInstantBeforeOrEqualTo(Instant instant, Instant other) {
        return !instant.isAfter(other);
    }

    public static boolean isInstantAfterOrEqualTo(Instant instant, Instant other) {
        return !instant.isBefore(other);
    }

    public static Instant toInstant(Temporal temporal) {
        if (temporal instanceof Instant) return (Instant) temporal;
        if (temporal instanceof ChronoLocalDateTime) return ((ChronoLocalDateTime<?>) temporal).atZone(ZoneId.systemDefault()).toInstant();
        if (temporal instanceof ChronoZonedDateTime) return ((ChronoZonedDateTime<?>) temporal).toInstant();
        if (temporal instanceof OffsetDateTime) return ((OffsetDateTime) temporal).toInstant();

        String unsupportedTemporalType = temporal == null ? "null" : temporal.getClass().getCanonicalName();
        throw new IllegalArgumentException("JobRunr does not support Temporal type: " + unsupportedTemporalType + ". Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
    }

}
