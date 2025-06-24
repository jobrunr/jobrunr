package org.jobrunr.scheduling.carbonaware;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.InstantUtils.toInstant;
import static org.mockito.InstantMocker.mockTime;

class CarbonAwarePeriodTest {

    @Test
    void testBeforeWithInstantAsTemporal() {
        var nowMorning = Instant.parse("2025-06-24T09:00:00Z");
        var nowNoon = Instant.parse("2025-06-24T12:00:00Z");
        try (var ignored = mockTime(nowMorning)) {
            var beforeNow = CarbonAwarePeriod.before(nowNoon);

            assertThat(beforeNow.getFrom()).isEqualTo(nowMorning);
            assertThat(beforeNow.getTo()).isEqualTo(nowNoon);
        }
    }

    @Test
    void testBeforeWithLocalDateTimeAsTemporal() {
        var zonedMorning = ZonedDateTime.parse("2025-05-27T10:00:00Z");
        var zonedNoon = ZonedDateTime.parse("2025-06-24T12:00:00Z");
        try (var ignored = mockTime(zonedMorning)) {
            var beforeNow = CarbonAwarePeriod.before(zonedNoon);

            assertThat(beforeNow.getFrom()).isEqualTo(toInstant(zonedMorning));
            assertThat(beforeNow.getTo()).isEqualTo(toInstant(zonedNoon));
        }
    }

    @Test
    void testBetweenWithInstantAsTemporal() {
        var zonedMorning = ZonedDateTime.parse("2025-05-27T10:00:00Z");
        var zonedNoon = ZonedDateTime.parse("2025-06-24T12:00:00Z");

        var beforeNow = CarbonAwarePeriod.between(zonedMorning, zonedNoon);

        assertThat(beforeNow.getFrom()).isEqualTo(toInstant(zonedMorning));
        assertThat(beforeNow.getTo()).isEqualTo(toInstant(zonedNoon));
    }

    @Test
    void testBetweenWithLocalDateTimeAsTemporal() {
        var nowMorning = Instant.parse("2025-06-24T09:00:00Z");
        var nowNoon = Instant.parse("2025-06-24T12:00:00Z");
        var beforeNow = CarbonAwarePeriod.between(nowMorning, nowNoon);

        assertThat(beforeNow.getFrom()).isEqualTo(nowMorning);
        assertThat(beforeNow.getTo()).isEqualTo(nowNoon);
    }

}