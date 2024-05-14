package org.jobrunr.utils;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.MICROS;

public class TimeUtils {


    public static long toMicroSeconds(Instant instant) {
        return MICROS.between(Instant.EPOCH, instant);
    }

    public static Instant fromMicroseconds(Long micros) {
        if (micros == null) {
            return null;
        }
        return Instant.ofEpochSecond(micros / 1_000_000, (micros % 1_000_000) * 1_000);
    }
}
