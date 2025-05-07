package org.jobrunr.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

import static java.time.ZoneId.systemDefault;

public class InstantUtils {

    private InstantUtils() {
    }

    public static Instant toInstant(Temporal temporal) {
        if (temporal instanceof Instant) return (Instant) temporal;
        if (temporal instanceof LocalDateTime) return ((LocalDateTime) temporal).atZone(systemDefault()).toInstant();
        if (temporal instanceof OffsetDateTime) return ((OffsetDateTime) temporal).toInstant();
        if (temporal instanceof ZonedDateTime) return ((ZonedDateTime) temporal).toInstant();
        throw new IllegalArgumentException("JobRunr does not support Temporal type: " + temporal.getClass() + ". Supported types are Instant, LocalDateTime, OffsetDateTime and ZonedDateTime.");
    }
}
