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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.utils.InstantUtils.isInstantAfterOrEqualTo;
import static org.jobrunr.utils.InstantUtils.isInstantBeforeOrEqualTo;
import static org.jobrunr.utils.InstantUtils.isInstantInPeriod;

class InstantUtilsTest {

    @Test
    void testIsInstantInPeriod() {
        Instant now = Instant.now();
        Instant startPeriod = now.minusSeconds(10);
        Instant endPeriod = now.plusSeconds(10);

        assertThat(isInstantInPeriod(now, startPeriod, endPeriod)).isTrue();

        assertThat(isInstantInPeriod(now.minusSeconds(20), startPeriod, endPeriod)).isFalse();
        assertThat(isInstantInPeriod(now.plusSeconds(20), startPeriod, endPeriod)).isFalse();
    }

    @Test
    void testIsInstantBeforeOrEqualTo() {
        Instant now = Instant.now();
        assertThat(isInstantBeforeOrEqualTo(now, now)).isTrue();
        assertThat(isInstantBeforeOrEqualTo(now, now.plusSeconds(1))).isTrue();

        assertThat(isInstantBeforeOrEqualTo(now, now.minusSeconds(1))).isFalse();
    }

    @Test
    void testIsInstantAfterOrEqualTo() {
        Instant now = Instant.now();
        assertThat(isInstantAfterOrEqualTo(now, now)).isTrue();
        assertThat(isInstantAfterOrEqualTo(now, now.minusSeconds(1))).isTrue();

        assertThat(isInstantAfterOrEqualTo(now, now.plusSeconds(1))).isFalse();
    }

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
}