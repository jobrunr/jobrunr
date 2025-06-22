package org.jobrunr.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

public class InstantUtils {

    private InstantUtils() {
    }

    public static Instant max(Collection<Instant> instants) {
        return max(instants.stream());
    }

    public static Instant max(Stream<Instant> instants) {
        return instants.filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
    }

    public static Instant max(Instant instant1, Instant instant2) {
        if(instant1 == null && instant2 == null) return null;
        if(instant1 == null) return instant2;
        if(instant2 == null) return instant1;
        return instant1.compareTo(instant2) > 0 ? instant1 : instant2;
    }

    public static boolean isInstantInPeriod(Instant instant, Instant startOfPeriod, Instant endOfPeriod) {
        return isInstantAfterOrEqualTo(instant, startOfPeriod) && instant.isBefore(endOfPeriod);
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
