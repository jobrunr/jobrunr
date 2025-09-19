package org.mockito;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.InstantMocker.mockTime;

class InstantMockerTest {

    @Test
    void testMockTimeForInstant() {
        try (MockedStatic<Instant> ignored = mockTime(Instant.parse("2025-05-27T09:00:00Z"))) {
            assertThat(Instant.now().toString()).isEqualTo("2025-05-27T09:00:00Z");
        }
    }

    @Test
    void testMockTimeForZonedDateTime() {
        try (MockedStaticHolder ignored = mockTime(ZonedDateTime.parse("2025-05-27T10:00:00Z"))) { // daily refresh time is at 19h if no data
            assertThat(Instant.now().toString()).isEqualTo("2025-05-27T10:00:00Z");
            assertThat(ZonedDateTime.now(ZoneId.systemDefault()).toString()).isEqualTo("2025-05-27T10:00Z");
        }
    }

}