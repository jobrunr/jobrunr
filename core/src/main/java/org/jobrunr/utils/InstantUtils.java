package org.jobrunr.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Objects;

public class InstantUtils {

    private InstantUtils() {
    }

    public static Instant toInstant(Temporal temporal) {
        if (temporal instanceof Instant) return (Instant) temporal;
        if (temporal instanceof ChronoLocalDateTime) return ((ChronoLocalDateTime<?>) temporal).atZone(ZoneId.systemDefault()).toInstant();
        if (temporal instanceof ChronoZonedDateTime) return ((ChronoZonedDateTime<?>) temporal).toInstant();
        if (temporal instanceof OffsetDateTime) return ((OffsetDateTime) temporal).toInstant();

        String unsupportedTemporalType = temporal == null ? "null" : temporal.getClass().getCanonicalName();
        throw new IllegalArgumentException("JobRunr does not support Temporal type: " + unsupportedTemporalType + ". Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
    }

    public static Instant max(Collection<Instant> instants) {
        return instants.stream().filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
    }
}
