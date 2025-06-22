package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.ThaiBuddhistDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InstantUtilsTest {

    @Test
    void toInstant() {
        var currentInstant = Instant.now();
        var currentLocalDateTime = LocalDateTime.now();
        var currentOffsetDateTime = OffsetDateTime.now();
        var currentZonedDateTime = ZonedDateTime.now();
        var currentHijraDateTime = HijrahDate.now().atTime(LocalTime.now());

        assertThat(InstantUtils.toInstant(currentInstant)).isEqualTo(currentInstant);
        assertThat(InstantUtils.toInstant(currentLocalDateTime)).isEqualTo(currentLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());
        assertThat(InstantUtils.toInstant(currentOffsetDateTime)).isEqualTo(currentOffsetDateTime.toInstant());
        assertThat(InstantUtils.toInstant(currentZonedDateTime)).isEqualTo(currentZonedDateTime.toInstant());
        assertThat(InstantUtils.toInstant(currentHijraDateTime)).isEqualTo(currentHijraDateTime.atZone(ZoneId.systemDefault()).toInstant());

        assertThatCode(() -> InstantUtils.toInstant(HijrahDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JobRunr does not support Temporal type: java.time.chrono.HijrahDate. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
        assertThatCode(() -> InstantUtils.toInstant(JapaneseDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JobRunr does not support Temporal type: java.time.chrono.JapaneseDate. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
        assertThatCode(() -> InstantUtils.toInstant(ThaiBuddhistDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JobRunr does not support Temporal type: java.time.chrono.ThaiBuddhistDate. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
        assertThatCode(() -> InstantUtils.toInstant(LocalTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JobRunr does not support Temporal type: java.time.LocalTime. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
        assertThatCode(() -> InstantUtils.toInstant(LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JobRunr does not support Temporal type: java.time.LocalDate. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
        assertThatCode(() -> InstantUtils.toInstant(Year.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JobRunr does not support Temporal type: java.time.Year. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
        assertThatCode(() -> InstantUtils.toInstant(YearMonth.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JobRunr does not support Temporal type: java.time.YearMonth. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
        assertThatCode(() -> InstantUtils.toInstant(OffsetTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JobRunr does not support Temporal type: java.time.OffsetTime. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
        assertThatCode(() -> InstantUtils.toInstant(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JobRunr does not support Temporal type: null. Supported types are Instant, ChronoLocalDateTime (e.g., LocalDateTime), ChronoZonedDateTime (e.g., ZonedDateTime) and OffsetDateTime.");
    }

    @Test
    void max() {
        assertThat(InstantUtils.max(new ArrayList<>())).isNull();

        Instant in2024 = Instant.parse("2024-11-20T09:00:00.000Z");
        Instant in2025 = Instant.parse("2025-05-20T13:00:00.000Z");

        assertThat(InstantUtils.max(List.of(in2025, in2024))).isEqualTo(in2025);
        assertThat(InstantUtils.max(List.of())).isNull();
    }
}