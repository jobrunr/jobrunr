package org.jobrunr.utils.carbonaware;

import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CarbonAwareManagerTest {
    static final CarbonAwareJobManager defaultCarbonAwareJobManager = new CarbonAwareJobManager(CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration(), new JacksonJsonMapper());

    @Test
    void testGetDailyRunTimeShouldGiveResultBetween18And19() {
        Instant result = defaultCarbonAwareJobManager.getDailyRunTime(18);
        ZonedDateTime expectedTime = ZonedDateTime.now(ZoneId.of("Europe/Brussels"))
                .withHour(18).withMinute(0).withSecond(0);

        assertTrue(result.isAfter(expectedTime.toInstant()) || result.equals(expectedTime.toInstant()));
        assertTrue(result.isBefore(expectedTime.plusHours(1).toInstant()));
    }

    @Test
    void testGetDailyRunTimeShouldNotGive2ConsecutiveEqualResults() {
        Instant firstCall = defaultCarbonAwareJobManager.getDailyRunTime(18);
        Instant secondCall = defaultCarbonAwareJobManager.getDailyRunTime(18);
        assertNotEquals(firstCall, secondCall);
    }

    @Test
    void testGetDailyRunTimeShouldGiveResultsin5SecondsBuckets() {
        Instant firstCall = defaultCarbonAwareJobManager.getDailyRunTime(18);
        Instant secondCall = defaultCarbonAwareJobManager.getDailyRunTime(18);
        assertEquals(0, firstCall.getEpochSecond() % 5);
        assertEquals(0, secondCall.getEpochSecond() % 5);
    }

}
