package org.jobrunr.scheduling.carbonaware;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;

class CarbonAwareTest {

    @Test
    void testCarbonAwareAt() {
        CarbonAwarePeriod carbonAwarePeriod = CarbonAware.at(Instant.parse("2025-06-25T10:00:00Z"), Duration.of(3, HOURS));
        assertThat(carbonAwarePeriod)
                .isNotNull()
                .hasFrom(Instant.parse("2025-06-25T07:00:00Z"))
                .hasTo(Instant.parse("2025-06-25T13:00:00Z"));
    }

    @Test
    void testCarbonAwareAtWithCarbonAwarePeriodThrowsException() {
        assertThatCode(() -> CarbonAware.at(new CarbonAwarePeriod(Instant.now(), Instant.now().plus(4, HOURS)), Duration.of(3, HOURS)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dailyBetween_invalidValuesThrowsException() {
        assertThatCode(() -> CarbonAware.dailyBetween(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> CarbonAware.dailyBetween(1, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> CarbonAware.dailyBetween(24, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> CarbonAware.dailyBetween(1, 24)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dailyBetween_crossesOverToNextDate_ThrowsException() {
        assertThatCode(() -> CarbonAware.dailyBetween(11, 11)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> CarbonAware.dailyBetween(23, 11)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dailyBetween_returnsScheduleStringWithCarbonBounds() {
        assertThat(CarbonAware.dailyBetween(11, 23)).isEqualTo("0 23 * * * [PT12H/PT0S]");
    }

}